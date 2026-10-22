package com.swu.gps.model.response;

public record LocationLikeResponse(
        long locationId,
        long likeCount,
        boolean liked
) {
}
