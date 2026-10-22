package com.example.gps.network.dto;

import java.util.List;

public class UserStatsDto {
    public long totalPlayCount;
    public long visitedLocationCount;
    public long autoPlayCount;
    public long manualPlayCount;
    public List<RecentVisitDto> recentVisits;
}
