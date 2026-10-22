package com.swu.gps.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShareCommentRequest(
        @NotBlank(message = "评论不能为空")
        @Size(max = 1000, message = "评论过长")
        String content
) {
}
