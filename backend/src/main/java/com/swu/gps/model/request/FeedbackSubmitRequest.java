package com.swu.gps.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackSubmitRequest(
        @NotBlank(message = "反馈内容不能为空")
        @Size(max = 2000, message = "反馈内容不能超过2000字")
        String content
) {
}
