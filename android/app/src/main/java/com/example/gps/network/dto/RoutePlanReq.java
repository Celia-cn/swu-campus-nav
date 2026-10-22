package com.example.gps.network.dto;

public class RoutePlanReq {
    public double startLatitude;
    public double startLongitude;
    public double endLatitude;
    public double endLongitude;
    public String routeMode;

    public RoutePlanReq(double startLat, double startLng, double endLat, double endLng, String mode) {
        this.startLatitude = startLat;
        this.startLongitude = startLng;
        this.endLatitude = endLat;
        this.endLongitude = endLng;
        this.routeMode = mode;
    }
}