package com.swu.gps.model.response;

import java.time.LocalDateTime;

public record TriggerHistoryResponse(
        Long id,
        Long locationId,
        String locationName,
        String triggerType,
        Double distanceMeters,
        Boolean success,
        LocalDateTime createdAt
) {}