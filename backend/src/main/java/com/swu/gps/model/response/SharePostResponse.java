package com.swu.gps.model.response;

import java.time.LocalDateTime;

public record SharePostResponse(
        Long id,
        Long userId,
        String username,
        String authorRole,
        String category,
        String title,
        String content,
        String imageUrl,
        boolean titleAsImage,
        int likeCount,
        int commentCount,
        boolean likedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
