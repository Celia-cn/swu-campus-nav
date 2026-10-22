package com.swu.gps.model.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserRouteDetail(
        Long id,
        String name,
        String description,
        Double totalDistanceMeters,
        Integer estimatedTimeMinutes,
        List<WaypointDetail> waypoints,
        Boolean isPublic,
        Integer likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record WaypointDetail(
            Long id,
            Integer waypointOrder,
            Double latitude,
            Double longitude,
            Long locationId,
            String locationName,
            Integer stayDurationMinutes,
            String notes
    ) {
    }
}
