package com.swu.gps.model.response;

import java.util.List;

public record UserStatsResponse(
        long totalPlayCount,
        long visitedLocationCount,
        long autoPlayCount,
        long manualPlayCount,
        List<RecentVisitItem> recentVisits
) {
    public record RecentVisitItem(String locationName, String visitedAt) {
    }
}
