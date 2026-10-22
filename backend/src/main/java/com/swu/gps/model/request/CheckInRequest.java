package com.swu.gps.model.request;

import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
        @NotNull(message = "latitude 不能为空")
        Double latitude,

        @NotNull(message = "longitude 不能为空")
        Double longitude
) {
}
