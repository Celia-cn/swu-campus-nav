package com.swu.gps.model.response;

import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long id,
        String username,
        String role,
        LocalDateTime createdAt
) {
}
