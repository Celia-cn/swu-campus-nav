package com.swu.gps.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RagUpsertRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题不能超过200字")
        String title,

        @NotBlank(message = "分类不能为空")
        String category,

        @NotBlank(message = "内容不能为空")
        @Size(max = 20000, message = "内容不能超过20000字")
        String content,

        @NotNull(message = "启用状态不能为空")
        Boolean enabled
) {
}
