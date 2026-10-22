package com.swu.gps.service;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.GuideLocation;
import com.swu.gps.model.entity.LocationLike;
import com.swu.gps.model.entity.TriggerHistory;
import com.swu.gps.model.entity.TriggerType;
import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.entity.VisitLog;
import com.swu.gps.model.request.CheckInRequest;
import com.swu.gps.model.request.LocationUpsertRequest;
import com.swu.gps.model.request.ManualPlayRequest;
import com.swu.gps.model.response.CheckInResponse;
import com.swu.gps.model.response.LocationLikeResponse;
import com.swu.gps.model.response.LocationResponse;
import com.swu.gps.model.response.TriggerHistoryResponse;
import com.swu.gps.model.response.TriggeredLocationResponse;
import com.swu.gps.repository.GuideLocationRepository;
import com.swu.gps.repository.LocationLikeRepository;
import com.swu.gps.repository.TriggerHistoryRepository;
import com.swu.gps.repository.UserAccountRepository;
import com.swu.gps.repository.VisitLogRepository;
import com.swu.gps.util.GeoUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final GuideLocationRepository guideLocationRepository;
    private final LocationLikeRepository locationLikeRepository;
    private final UserAccountRepository userAccountRepository;
    private final VisitLogRepository visitLogRepository;

    private final TriggerHistoryRepository triggerHistoryRepository;
    private final LocationMediaService locationMediaService;

    public LocationService(GuideLocationRepository guideLocationRepository,
                           LocationLikeRepository locationLikeRepository,
                           UserAccountRepository userAccountRepository,
                           VisitLogRepository visitLogRepository,
                           TriggerHistoryRepository triggerHistoryRepository,
                           LocationMediaService locationMediaService) {
        this.guideLocationRepository = guideLocationRepository;
        this.locationLikeRepository = locationLikeRepository;
        this.userAccountRepository = userAccountRepository;
        this.visitLogRepository = visitLogRepository;
        this.triggerHistoryRepository = triggerHistoryRepository;
        this.locationMediaService = locationMediaService;
    }

    public List<LocationResponse> listLocations(Long userId) {
        List<GuideLocation> locations = guideLocationRepository.findAllByOrderByLikeCountDescIdAsc();
        Set<Long> likedIds = resolveLikedLocationIds(userId, locations);
        return locations.stream()
                .map(location -> toLocationResponse(location, likedIds.contains(location.getId())))
                .collect(Collectors.toList());
    }

    public LocationResponse getLocation(Long id, Long userId) {
        GuideLocation location = findLocation(id);
        boolean liked = userId != null
                && locationLikeRepository.existsByUser_IdAndLocation_Id(userId, id);
        return toLocationResponse(location, liked);
    }

    @Transactional
    public LocationLikeResponse toggleLike(UserAccount user, Long locationId) {
        GuideLocation location = findLocation(locationId);
        Long userId = user.getId();
        int count = safeLikeCount(location);
        boolean liked;
        if (locationLikeRepository.existsByUser_IdAndLocation_Id(userId, locationId)) {
            locationLikeRepository.deleteByUser_IdAndLocation_Id(userId, locationId);
            count = Math.max(0, count - 1);
            liked = false;
        } else {
            LocationLike like = new LocationLike();
            like.setUser(userAccountRepository.getReferenceById(userId));
            like.setLocation(location);
            like.setCreatedAt(LocalDateTime.now());
            locationLikeRepository.save(like);
            count = count + 1;
            liked = true;
        }
        location.setLikeCount(count);
        location.setUpdatedAt(LocalDateTime.now());
        guideLocationRepository.save(location);
        return new LocationLikeResponse(locationId, count, liked);
    }

    @Transactional
    public CheckInResponse checkIn(UserAccount user, CheckInRequest request) {
        List<GuideLocation> locations = guideLocationRepository.findAll();
        List<DistanceCandidate> candidates = new ArrayList<>();

        for (GuideLocation location : locations) {
            double distance = GeoUtils.distanceMeters(
                    request.latitude(),
                    request.longitude(),
                    location.getLatitude(),
                    location.getLongitude()
            );
            if (distance <= location.getRadiusMeters()) {
                candidates.add(new DistanceCandidate(location, distance));
            }
        }

        candidates.sort(Comparator.comparingDouble(DistanceCandidate::distance));

        List<TriggeredLocationResponse> triggered = candidates.stream()
                .map(it -> new TriggeredLocationResponse(
                        it.location().getId(),
                        it.location().getName(),
                        round2(it.distance()),
                        it.location().getAudioUrl(),
                        it.location().getDescription()
                ))
                .collect(Collectors.toList());

        LocationResponse recommended = null;
        boolean shouldAutoPlay = !candidates.isEmpty();

        if (shouldAutoPlay) {
            DistanceCandidate nearest = candidates.get(0);
            boolean liked = locationLikeRepository.existsByUser_IdAndLocation_Id(
                    user.getId(), nearest.location().getId());
            recommended = toLocationResponse(nearest.location(), liked);
            saveVisit(user, nearest.location(), TriggerType.AUTO,
                    request.latitude(), request.longitude(), nearest.distance());
        }

        return new CheckInResponse(shouldAutoPlay, recommended, triggered);
    }

    @Transactional
    public void recordManualPlay(UserAccount user, Long locationId, ManualPlayRequest request) {
        GuideLocation location = findLocation(locationId);
        double lat = request != null && request.latitude() != null ? request.latitude() : location.getLatitude();
        double lng = request != null && request.longitude() != null ? request.longitude() : location.getLongitude();
        double distance = GeoUtils.distanceMeters(lat, lng, location.getLatitude(), location.getLongitude());

        saveVisit(user, location, TriggerType.MANUAL, lat, lng, distance);
    }

    @Transactional
    public LocationResponse createLocation(LocationUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now();
        GuideLocation location = new GuideLocation();
        applyRequest(location, request, now, now);
        guideLocationRepository.save(location);
        return toLocationResponse(location, false);
    }

    @Transactional
    public LocationResponse updateLocation(Long id, LocationUpsertRequest request) {
        GuideLocation location = findLocation(id);
        applyRequest(location, request, location.getCreatedAt(), LocalDateTime.now());
        guideLocationRepository.save(location);
        return toLocationResponse(location, false);
    }

    @Transactional
    public void deleteLocation(Long id) {
        GuideLocation location = findLocation(id);
        locationMediaService.deleteAllMediaForLocation(location);
        guideLocationRepository.delete(location);
    }

    private void saveVisit(UserAccount user,
                           GuideLocation location,
                           TriggerType triggerType,
                           double userLat,
                           double userLng,
                           double distanceMeters) {
        VisitLog log = new VisitLog();
        log.setUser(user);
        log.setLocation(location);
        log.setTriggerType(triggerType);
        log.setUserLatitude(userLat);
        log.setUserLongitude(userLng);
        log.setDistanceMeters(round2(distanceMeters));
        log.setCreatedAt(LocalDateTime.now());
        visitLogRepository.save(log);
    }

    private GuideLocation findLocation(Long id) {
        return guideLocationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "讲解点不存在: " + id));
    }

    private Set<Long> resolveLikedLocationIds(Long userId, List<GuideLocation> locations) {
        if (userId == null || locations.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = locations.stream().map(GuideLocation::getId).collect(Collectors.toList());
        return new HashSet<>(locationLikeRepository.findLikedLocationIds(userId, ids));
    }

    private int safeLikeCount(GuideLocation location) {
        return location.getLikeCount() == null ? 0 : Math.max(0, location.getLikeCount());
    }

    private LocationResponse toLocationResponse(GuideLocation location, boolean liked) {
        List<String> imageUrls = location.getImageUrls();
        return new LocationResponse(
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
                safeLikeCount(location),
                liked
        );
    }

    private void applyRequest(GuideLocation location,
                              LocationUpsertRequest request,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        location.setName(request.name());
        location.setCategory(request.category());
        location.setAddress(request.address());
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setRadiusMeters(request.radiusMeters());
        if (request.cooldownSeconds() != null) {
            location.setCooldownSeconds(request.cooldownSeconds());
        } else if (location.getCooldownSeconds() == null) {
            location.setCooldownSeconds(120);
        }
        location.setDescription(request.description());
        if (request.audioUrl() != null) {
            location.setAudioUrl(request.audioUrl().isBlank() ? null : request.audioUrl().trim());
        }
        if (request.imageUrl() != null) {
            String single = request.imageUrl().isBlank() ? null : request.imageUrl().trim();
            if (single == null) {
                location.setImageUrls(List.of());
            } else {
                location.setImageUrls(List.of(single));
            }
        }
        if (location.getLikeCount() == null) {
            location.setLikeCount(0);
        }
        location.setCreatedAt(createdAt);
        location.setUpdatedAt(updatedAt);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record DistanceCandidate(GuideLocation location, double distance) {
    }

    /**
     * 检查是否应该触发讲解
     */
    public TriggerCheckResult shouldTriggerExplanation(UserAccount user, GuideLocation location,
                                                       double userLat, double userLng, double distance) {
        if (distance > location.getRadiusMeters()) {
            return new TriggerCheckResult(false, "距离超出范围");
        }

        Integer cooldownSeconds = location.getCooldownSeconds() != null ?
                location.getCooldownSeconds() : 120;
        LocalDateTime lastTrigger = triggerHistoryRepository.findLastTriggerTime(
                user.getId(), location.getId());

        if (lastTrigger != null) {
            long secondsSinceLastTrigger = java.time.Duration.between(lastTrigger, LocalDateTime.now()).getSeconds();
            if (secondsSinceLastTrigger < cooldownSeconds) {
                return new TriggerCheckResult(false,
                        "冷却中，还需等待" + (cooldownSeconds - secondsSinceLastTrigger) + "秒");
            }
        }

        if (location.getMaxTriggerCount() != null) {
            long triggerCount = triggerHistoryRepository.countByUserIdAndLocationId(
                    user.getId(), location.getId());
            if (triggerCount >= location.getMaxTriggerCount()) {
                return new TriggerCheckResult(false, "已达到最大触发次数");
            }
        }

        return new TriggerCheckResult(true, null);
    }

    /**
     * 记录触发历史
     */
    @Transactional
    public void recordTriggerHistory(UserAccount user, GuideLocation location,
                                     TriggerType triggerType, double userLat, double userLng,
                                     double distance, boolean success, String failureReason,
                                     Integer stayDuration) {
        TriggerHistory history = new TriggerHistory();
        history.setUser(user);
        history.setLocation(location);
        history.setTriggerType(triggerType);
        history.setUserLatitude(userLat);
        history.setUserLongitude(userLng);
        history.setDistanceMeters(Math.round(distance * 100.0) / 100.0);
        history.setStayDurationSeconds(stayDuration);
        history.setSuccess(success);
        history.setFailureReason(failureReason);
        history.setCreatedAt(LocalDateTime.now());

        triggerHistoryRepository.save(history);
    }

    /**
     * 获取用户在指定讲解点的触发历史
     */
    public List<TriggerHistoryResponse> getUserTriggerHistory(UserAccount user, Long locationId, int limit) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, limit);
        List<TriggerHistory> histories = triggerHistoryRepository
                .findByUserIdAndLocationIdOrderByCreatedAtDesc(user.getId(), locationId, pageable);

        return histories.stream()
                .map(h -> new TriggerHistoryResponse(
                        h.getId(),
                        h.getLocation().getId(),
                        h.getLocation().getName(),
                        h.getTriggerType().name(),
                        h.getDistanceMeters(),
                        h.getSuccess(),
                        h.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // 内部类
    public static class TriggerCheckResult {
        private final boolean shouldTrigger;
        private final String reason;

        public TriggerCheckResult(boolean shouldTrigger, String reason) {
            this.shouldTrigger = shouldTrigger;
            this.reason = reason;
        }

        public boolean shouldTrigger() { return shouldTrigger; }
        public String getReason() { return reason; }
    }
}
