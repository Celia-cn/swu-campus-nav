package com.example.gps.network.dto;

import java.util.List;

public class CheckInDto {
    public boolean shouldAutoPlay;
    public LocationDto recommendedLocation;
    public List<TriggeredLocationDto> triggeredLocations;
}
