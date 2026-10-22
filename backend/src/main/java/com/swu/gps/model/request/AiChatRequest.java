package com.swu.gps.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiChatRequest(
        @NotBlank(message = "消息不能为空")
        @Size(max = 2000, message = "消息不能超过2000字")
        String message,

        List<ChatMessageItem> history
) {
    public record ChatMessageItem(
            @NotBlank String role,
            @NotBlank String content
    ) {
    }
}
