package com.swu.gps.controller;

import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.request.LoginRequest;
import com.swu.gps.model.request.RegisterRequest;
import com.swu.gps.model.request.ResetPasswordRequest;
import com.swu.gps.model.response.ApiResponse;
import com.swu.gps.model.response.AuthResponse;
import com.swu.gps.model.response.UserProfileResponse;
import com.swu.gps.model.response.UserStatsResponse;
import com.swu.gps.service.AuthService;
import com.swu.gps.service.UserStatsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserStatsService userStatsService;

    public AuthController(AuthService authService, UserStatsService userStatsService) {
        this.authService = authService;
        this.userStatsService = userStatsService;
    }

    /** 手机端注册：仅创建普通用户 */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.registerUser(request));
    }

    /** 管理后台注册：仅创建管理员 */
    @PostMapping("/admin/register")
    public ApiResponse<AuthResponse> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("管理员注册成功", authService.registerAdmin(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("密码已重置", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return ApiResponse.success(authService.toUserProfile(user));
    }

    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> myStats(HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return ApiResponse.success(userStatsService.getUserStats(user));
    }
}
