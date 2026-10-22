package com.swu.gps.model.response;

import java.util.List;

public record LocationResponse(
        Long id,
        String name,
        String category,
        String address,
        Double latitude,
        Double longitude,
        Integer radiusMeters,
        Integer cooldownSeconds,
        String description,
        String audioUrl,
        String imageUrl,
        List<String> imageUrls,
        long likeCount,
        boolean liked
) {
}
