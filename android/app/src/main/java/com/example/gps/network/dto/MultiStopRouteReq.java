package com.example.gps.network.dto;

import java.util.List;

public class MultiStopRouteReq {
    public Double startLatitude;
    public Double startLongitude;
    public List<Long> locationIds;
    public String routeMode;

    public MultiStopRouteReq(Double startLat, Double startLng, List<Long> ids, String mode) {
        this.startLatitude = startLat;
        this.startLongitude = startLng;
        this.locationIds = ids;
        this.routeMode = mode;
    }
}
