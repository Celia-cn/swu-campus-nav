package com.swu.gps.model.response;

public record HotLocationResponse(
        Long locationId,
        String locationName,
        Long playCount
) {
}
