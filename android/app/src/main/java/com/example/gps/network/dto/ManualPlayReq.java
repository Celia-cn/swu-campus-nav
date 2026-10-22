package com.example.gps.network.dto;

public class ManualPlayReq {
    public Double latitude;
    public Double longitude;

    public ManualPlayReq(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
