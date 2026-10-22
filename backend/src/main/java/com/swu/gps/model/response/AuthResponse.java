package com.swu.gps.model.response;

import java.time.LocalDateTime;

public record AuthResponse(
        String token,
        LocalDateTime expiresAt,
        UserProfileResponse user
) {
}
