package com.swu.gps.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SharePostUpsertRequest(
        @NotBlank(message = "分类不能为空")
        String category,
        @NotBlank(message = "标题不能为空")
        @Size(max = 120, message = "标题过长")
        String title,
        @NotBlank(message = "内容不能为空")
        @Size(max = 2000, message = "内容过长")
        String content,
        @NotBlank(message = "配图地址不能为空")
        @Size(max = 500, message = "配图地址过长")
        String imageUrl,
        Boolean titleAsImage
) {
}
