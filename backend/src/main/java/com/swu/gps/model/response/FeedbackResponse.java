package com.swu.gps.model.response;

import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        Long userId,
        String username,
        String content,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
