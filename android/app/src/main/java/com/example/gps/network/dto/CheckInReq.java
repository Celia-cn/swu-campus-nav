package com.example.gps.network.dto;

public class CheckInReq {
    public double latitude;
    public double longitude;

    public CheckInReq(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
