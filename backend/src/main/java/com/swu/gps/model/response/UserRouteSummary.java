package com.swu.gps.model.response;

import java.time.LocalDateTime;

public record UserRouteSummary(
        Long id,
        String name,
        String description,
        Double totalDistanceMeters,
        Integer estimatedTimeMinutes,
        Integer waypointCount,
        Boolean isPublic,
        Integer likeCount,
        LocalDateTime createdAt
) {
}
