package com.swu.gps.model.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MultiStopRouteRequest(
        Double startLatitude,
        Double startLongitude,

        @NotEmpty(message = "locationIds 不能为空")
        List<Long> locationIds,

        String routeMode
) {
}
