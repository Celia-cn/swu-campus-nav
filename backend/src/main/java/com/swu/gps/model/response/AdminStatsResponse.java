package com.swu.gps.model.response;

import java.util.List;

public record AdminStatsResponse(
        long totalUsers,
        long totalLocations,
        long totalPlayCount,
        long todayVisitors,
        long todayNewUsers,
        long todayPlayCount,
        long weekPlayCount,
        long autoPlayCount,
        long manualPlayCount,
        List<HotLocationResponse> hotLocations,
        List<VisitHourStatResponse> visitHourStats
) {
}
