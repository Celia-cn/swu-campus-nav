package com.example.gps.network.dto;

public class LocationUpsertReq {
    public String name;
    public String category;
    public String address;
    public Double latitude;
    public Double longitude;
    public Integer radiusMeters;
    public Integer cooldownSeconds;
    public String description;
    public String audioUrl;
    public String imageUrl;

    public LocationUpsertReq(String name,
                             String category,
                             String address,
                             Double latitude,
                             Double longitude,
                             Integer radiusMeters,
                             Integer cooldownSeconds,
                             String description,
                             String audioUrl,
                             String imageUrl) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.cooldownSeconds = cooldownSeconds;
        this.description = description;
        this.audioUrl = audioUrl;
        this.imageUrl = imageUrl;
    }
}
