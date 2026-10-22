package com.swu.gps.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LocationUpsertRequest(
        @NotBlank(message = "名称不能为空")
        @Size(max = 100, message = "名称长度不能超过100")
        String name,

        @NotBlank(message = "分类不能为空")
        @Size(max = 40, message = "分类长度不能超过40")
        String category,

        @Size(max = 255, message = "地址长度不能超过255")
        String address,

        @NotNull(message = "latitude 不能为空")
        Double latitude,

        @NotNull(message = "longitude 不能为空")
        Double longitude,

        @NotNull(message = "触发半径不能为空")
        @Min(value = 10, message = "触发半径不能小于10米")
        @Max(value = 1000, message = "触发半径不能大于1000米")
        Integer radiusMeters,

        @Min(value = 30, message = "冷却时间不能小于30秒")
        @Max(value = 3600, message = "冷却时间不能大于3600秒")
        Integer cooldownSeconds,

        @NotBlank(message = "介绍内容不能为空")
        @Size(max = 2000, message = "介绍内容过长")
        String description,

        @Size(max = 500, message = "audioUrl 过长")
        String audioUrl,

        @Size(max = 500, message = "imageUrl 过长")
        String imageUrl
) {
}
