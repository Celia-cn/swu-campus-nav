package com.swu.gps.model.response;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String role,
        LocalDateTime createdAt
) {
}
