package com.swu.gps.service;

import com.swu.gps.model.entity.TriggerType;
import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.response.UserStatsResponse;
import com.swu.gps.repository.VisitLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserStatsService {

    private final VisitLogRepository visitLogRepository;

    public UserStatsService(VisitLogRepository visitLogRepository) {
        this.visitLogRepository = visitLogRepository;
    }

    public UserStatsResponse getUserStats(UserAccount user) {
        long userId = user.getId();
        long total = visitLogRepository.countByUser_Id(userId);
        long visited = visitLogRepository.countDistinctLocationsByUserId(userId);
        long auto = visitLogRepository.countByUser_IdAndTriggerType(userId, TriggerType.AUTO);
        long manual = visitLogRepository.countByUser_IdAndTriggerType(userId, TriggerType.MANUAL);

        List<UserStatsResponse.RecentVisitItem> recent = visitLogRepository
                .findRecentByUserId(userId, PageRequest.of(0, 5))
                .stream()
                .map(row -> new UserStatsResponse.RecentVisitItem(
                        row.getLocationName(),
                        row.getVisitedAt() == null ? "" : row.getVisitedAt().toString()))
                .collect(Collectors.toList());

        return new UserStatsResponse(total, visited, auto, manual, recent);
    }
}
