package com.swu.gps.model.response;

import java.time.LocalDateTime;

public record RagDocumentResponse(
        Long id,
        String title,
        String category,
        String content,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
