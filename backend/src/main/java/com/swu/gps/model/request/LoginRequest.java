package com.swu.gps.model.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password,

        /** MOBILE：手机端（任意角色）；WEB：管理后台（仅管理员） */
        String client
) {
}
