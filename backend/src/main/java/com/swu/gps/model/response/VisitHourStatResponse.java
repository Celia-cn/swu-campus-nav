package com.swu.gps.model.response;

public record VisitHourStatResponse(
        int hour,
        long visitorCount
) {
}
