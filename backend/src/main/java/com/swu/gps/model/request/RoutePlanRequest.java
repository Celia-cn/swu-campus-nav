package com.swu.gps.model.request;

import jakarta.validation.constraints.NotNull;

public record RoutePlanRequest(
        @NotNull(message = "startLatitude 不能为空")
        Double startLatitude,

        @NotNull(message = "startLongitude 不能为空")
        Double startLongitude,

        @NotNull(message = "endLatitude 不能为空")
        Double endLatitude,

        @NotNull(message = "endLongitude 不能为空")
        Double endLongitude,

        String routeMode
) {
}

