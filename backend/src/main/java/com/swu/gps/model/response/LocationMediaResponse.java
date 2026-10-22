package com.swu.gps.model.response;

import java.util.List;

public record LocationMediaResponse(
        List<String> imageUrls,
        String audioUrl
) {
}
