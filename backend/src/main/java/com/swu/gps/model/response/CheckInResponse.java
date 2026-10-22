package com.swu.gps.model.response;

import java.util.List;

public record CheckInResponse(
        boolean shouldAutoPlay,
        LocationResponse recommendedLocation,
        List<TriggeredLocationResponse> triggeredLocations
) {
}
