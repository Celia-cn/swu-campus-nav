package com.example.gps.network.dto;

import java.util.List;

public class AdminStatsDto {
    public long totalUsers;
    public long totalLocations;
    public long totalPlayCount;
    public long todayPlayCount;
    public long autoPlayCount;
    public long manualPlayCount;
    public List<HotLocationDto> hotLocations;
}
