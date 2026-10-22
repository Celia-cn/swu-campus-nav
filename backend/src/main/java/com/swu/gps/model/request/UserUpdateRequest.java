package com.swu.gps.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "角色不能为空")
        String role,

        @Size(min = 6, max = 64, message = "密码长度为6-64位")
        String password
) {
}
