package com.swu.gps.model.response;

public record TriggeredLocationResponse(
        Long locationId,
        String name,
        Double distanceMeters,
        String audioUrl,
        String description
) {
}
