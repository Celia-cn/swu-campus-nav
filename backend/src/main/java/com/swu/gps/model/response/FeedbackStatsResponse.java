package com.swu.gps.model.response;

public record FeedbackStatsResponse(
        long totalCount,
        long todayCount,
        long pendingCount,
        long completedCount
) {
}
