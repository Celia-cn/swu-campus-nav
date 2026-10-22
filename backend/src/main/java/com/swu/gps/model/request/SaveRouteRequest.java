package com.swu.gps.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SaveRouteRequest(
        @NotBlank(message = "路线名称不能为空")
        String name,

        String description,
        Boolean isPublic,

        @NotEmpty(message = "途经点不能为空")
        List<WaypointRequest> waypoints
) {
    public record WaypointRequest(
            @NotNull(message = "地点ID不能为空")
            Long locationId,

            Double latitude,
            Double longitude,
            Integer stayDurationMinutes,
            String notes
    ) {
    }
}