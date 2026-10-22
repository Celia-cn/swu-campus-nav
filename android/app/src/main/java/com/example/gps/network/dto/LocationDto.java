package com.example.gps.network.dto;

import java.util.List;

public class LocationDto {
    public long id;
    public String name;
    public String category;
    public String address;
    public double latitude;
    public double longitude;
    public int radiusMeters;
    public int cooldownSeconds;
    public String description;
    public String audioUrl;
    public String imageUrl;
    public List<String> imageUrls;
    public long likeCount;
    public boolean liked;
}
