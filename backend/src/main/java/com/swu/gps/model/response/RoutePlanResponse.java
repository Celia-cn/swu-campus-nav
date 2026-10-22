package com.swu.gps.model.response;

import java.util.List;

public record RoutePlanResponse(
        List<GeoPointDto> routePoints,
        Double totalDistanceMeters,
        Integer estimatedTimeMinutes,
        List<RouteInstruction> instructions
) {
    public record GeoPointDto(
            Double latitude,
            Double longitude
    ) {
    }

    public record RouteInstruction(
            Integer step,
            Double latitude,
            Double longitude,
            String instruction
    ) {
    }
}
