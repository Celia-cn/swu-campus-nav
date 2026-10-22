package com.swu.gps.model.response;

import java.time.LocalDateTime;

public record ShareCommentResponse(
        Long id,
        Long userId,
        String username,
        String content,
        LocalDateTime createdAt
) {
}
