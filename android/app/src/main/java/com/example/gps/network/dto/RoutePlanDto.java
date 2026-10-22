package com.example.gps.network.dto;

import java.util.List;

public class RoutePlanDto {
    public List<GeoPointDto> routePoints;
    public double totalDistanceMeters;
    public int estimatedTimeMinutes;
    public List<RouteInstructionDto> instructions;

    public static class GeoPointDto {
        public double latitude;
        public double longitude;
    }

    public static class RouteInstructionDto {
        public int step;
        public double latitude;
        public double longitude;
        public String instruction;
        /** 兼容旧字段名 */
        public String description;
    }
}
