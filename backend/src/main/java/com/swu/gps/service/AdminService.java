package com.swu.gps.service;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.TriggerType;
import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.entity.UserRole;
import com.swu.gps.model.request.UserUpdateRequest;
import com.swu.gps.model.entity.GuideLocation;
import com.swu.gps.model.response.AdminLocationResponse;
import com.swu.gps.model.response.AdminStatsResponse;
import com.swu.gps.model.response.HotLocationResponse;
import com.swu.gps.model.response.UserSummaryResponse;
import com.swu.gps.model.response.VisitHourStatResponse;
import com.swu.gps.repository.GuideLocationRepository;
import com.swu.gps.repository.UserAccountRepository;
import com.swu.gps.repository.UserFeedbackRepository;
import com.swu.gps.repository.UserSessionRepository;
import com.swu.gps.repository.VisitLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AdminService {

    private final UserAccountRepository userAccountRepository;
    private final GuideLocationRepository guideLocationRepository;
    private final VisitLogRepository visitLogRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserFeedbackRepository userFeedbackRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserAccountRepository userAccountRepository,
                        GuideLocationRepository guideLocationRepository,
                        VisitLogRepository visitLogRepository,
                        UserSessionRepository userSessionRepository,
                        UserFeedbackRepository userFeedbackRepository,
                        PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.guideLocationRepository = guideLocationRepository;
        this.visitLogRepository = visitLogRepository;
        this.userSessionRepository = userSessionRepository;
        this.userFeedbackRepository = userFeedbackRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminLocationResponse> listLocationsWithPlayCount() {
        Map<Long, Long> playCountMap = visitLogRepository.findPlayCountGroupByLocation().stream()
                .collect(Collectors.toMap(
                        row -> row.getLocationId(),
                        row -> row.getPlayCount(),
                        (a, b) -> a));

        return guideLocationRepository.findAll().stream()
                .sorted(Comparator.comparing(GuideLocation::getId))
                .map(location -> {
                    var imageUrls = location.getImageUrls();
                    return new AdminLocationResponse(
                            location.getId(),
                            location.getName(),
                            location.getCategory(),
                            location.getAddress(),
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getRadiusMeters(),
                            location.getCooldownSeconds() != null ? location.getCooldownSeconds() : 120,
                            location.getDescription(),
                            location.getAudioUrl(),
                            imageUrls.isEmpty() ? location.getImageUrl() : imageUrls.get(0),
                            imageUrls,
                            playCountMap.getOrDefault(location.getId(), 0L));
                })
                .collect(Collectors.toList());
    }

    public List<UserSummaryResponse> listUsers() {
        return userAccountRepository.findAll().stream()
                .map(user -> new UserSummaryResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getRole().name(),
                        user.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public AdminStatsResponse getStats() {
        long totalUsers = userAccountRepository.count();
        long totalLocations = guideLocationRepository.count();
        long totalPlayCount = visitLogRepository.count();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayVisitors = visitLogRepository.countDistinctUsersAfter(startOfDay);
        long todayNewUsers = userAccountRepository.countByCreatedAtGreaterThanEqual(startOfDay);
        long todayPlayCount = visitLogRepository.countByCreatedAtAfter(startOfDay);
        LocalDateTime startOfWeek = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
        long weekPlayCount = visitLogRepository.countByCreatedAtGreaterThanEqual(startOfWeek);

        long autoPlayCount = visitLogRepository.countByTriggerType(TriggerType.AUTO);
        long manualPlayCount = visitLogRepository.countByTriggerType(TriggerType.MANUAL);

        List<HotLocationResponse> hotLocations = visitLogRepository.findHotLocations(PageRequest.of(0, 5)).stream()
                .map(row -> new HotLocationResponse(row.getLocationId(), row.getLocationName(), row.getPlayCount()))
                .collect(Collectors.toList());

        Map<Integer, Long> hourMap = new HashMap<>();
        for (VisitLogRepository.VisitHourProjection row : visitLogRepository.countVisitsByHour()) {
            if (row.getHourSlot() != null) {
                hourMap.put(row.getHourSlot(), row.getVisitCount() != null ? row.getVisitCount() : 0L);
            }
        }
        List<VisitHourStatResponse> visitHourStats = IntStream.range(0, 24)
                .mapToObj(hour -> new VisitHourStatResponse(hour, hourMap.getOrDefault(hour, 0L)))
                .collect(Collectors.toList());

        return new AdminStatsResponse(
                totalUsers,
                totalLocations,
                totalPlayCount,
                todayVisitors,
                todayNewUsers,
                todayPlayCount,
                weekPlayCount,
                autoPlayCount,
                manualPlayCount,
                hotLocations,
                visitHourStats
        );
    }

    @Transactional
    public UserSummaryResponse updateUser(Long id, UserUpdateRequest request, UserAccount operator) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));

        UserRole newRole = parseRole(request.role());
        if (user.getRole() == UserRole.ADMIN && newRole == UserRole.USER) {
            long adminCount = userAccountRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "至少保留一名管理员");
            }
        }
        user.setRole(newRole);

        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
            userSessionRepository.deleteByUser_Id(user.getId());
        }

        userAccountRepository.save(user);
        return toSummary(user);
    }

    @Transactional
    public void deleteUser(Long id, UserAccount operator) {
        if (operator.getId().equals(id)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能删除当前登录账号");
        }

        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (user.getRole() == UserRole.ADMIN) {
            long adminCount = userAccountRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "至少保留一名管理员");
            }
        }

        visitLogRepository.deleteByUser_Id(id);
        userFeedbackRepository.deleteByUser_Id(id);
        userSessionRepository.deleteByUser_Id(id);
        userAccountRepository.delete(user);
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "角色无效，仅支持 USER 或 ADMIN");
        }
    }

    private UserSummaryResponse toSummary(UserAccount user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getCreatedAt());
    }
}
