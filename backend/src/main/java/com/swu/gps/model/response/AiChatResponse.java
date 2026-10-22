package com.swu.gps.model.response;

import java.util.List;

public record AiChatResponse(
        String reply,
        List<String> sources
) {
}
