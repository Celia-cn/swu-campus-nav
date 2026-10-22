package com.example.gps.network.dto;

import java.util.List;

public class UserRouteDetailDto {
    public long id;
    public String name;
    public String description;
    public Double totalDistanceMeters;
    public Integer estimatedTimeMinutes;
    public List<WaypointDto> waypoints;
    public boolean isPublic;
    public int likeCount;
    public String createdAt;
    public String updatedAt;

    public static class WaypointDto {
        public long id;
        public int waypointOrder;
        public double latitude;
        public double longitude;
        public Long locationId;
        public String locationName;
        public Integer stayDurationMinutes;
        public String notes;
    }
}
