package com.swu.gps.controller;

import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.request.LocationUpsertRequest;
import com.swu.gps.model.request.UserUpdateRequest;
import com.swu.gps.model.response.AdminLocationResponse;
import com.swu.gps.model.response.AdminStatsResponse;
import com.swu.gps.model.response.ApiResponse;
import com.swu.gps.model.response.FeedbackResponse;
import com.swu.gps.model.response.FeedbackStatsResponse;
import com.swu.gps.model.response.LocationResponse;
import com.swu.gps.model.response.UserSummaryResponse;
import com.swu.gps.service.AdminService;
import com.swu.gps.service.AuthService;
import com.swu.gps.service.FeedbackService;
import com.swu.gps.service.LocationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthService authService;
    private final LocationService locationService;
    private final AdminService adminService;
    private final FeedbackService feedbackService;

    public AdminController(AuthService authService,
                           LocationService locationService,
                           AdminService adminService,
                           FeedbackService feedbackService) {
        this.authService = authService;
        this.locationService = locationService;
        this.adminService = adminService;
        this.feedbackService = feedbackService;
    }

    @GetMapping("/locations")
    public ApiResponse<List<AdminLocationResponse>> locations(HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success(adminService.listLocationsWithPlayCount());
    }

    @GetMapping("/users")
    public ApiResponse<List<UserSummaryResponse>> users(HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success(adminService.listUsers());
    }

    @PutMapping("/users/{id}")
    public ApiResponse<UserSummaryResponse> updateUser(@PathVariable Long id,
                                                       @Valid @RequestBody UserUpdateRequest body,
                                                       HttpServletRequest request) {
        UserAccount operator = ensureAdmin(request);
        return ApiResponse.success("用户更新成功", adminService.updateUser(id, body, operator));
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        UserAccount operator = ensureAdmin(request);
        adminService.deleteUser(id, operator);
        return ApiResponse.success("用户已删除", null);
    }

    @GetMapping("/stats")
    public ApiResponse<AdminStatsResponse> stats(HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success(adminService.getStats());
    }

    @GetMapping("/feedbacks")
    public ApiResponse<List<FeedbackResponse>> feedbacks(HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success(feedbackService.listAll());
    }

    @GetMapping("/feedbacks/stats")
    public ApiResponse<FeedbackStatsResponse> feedbackStats(HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success(feedbackService.getStats());
    }

    @PatchMapping("/feedbacks/{id:\\d+}/complete")
    public ApiResponse<FeedbackResponse> completeFeedback(@PathVariable Long id, HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success("已标记为已完成", feedbackService.markCompleted(id));
    }

    @DeleteMapping("/feedbacks/{id:\\d+}")
    public ApiResponse<Void> deleteFeedback(@PathVariable Long id, HttpServletRequest request) {
        ensureAdmin(request);
        feedbackService.delete(id);
        return ApiResponse.success("反馈已删除", null);
    }

    @PostMapping("/locations")
    public ApiResponse<LocationResponse> createLocation(@Valid @RequestBody LocationUpsertRequest request,
                                                        HttpServletRequest httpRequest) {
        ensureAdmin(httpRequest);
        return ApiResponse.success("讲解点创建成功", locationService.createLocation(request));
    }

    @PutMapping("/locations/{id}")
    public ApiResponse<LocationResponse> updateLocation(@PathVariable Long id,
                                                        @Valid @RequestBody LocationUpsertRequest request,
                                                        HttpServletRequest httpRequest) {
        ensureAdmin(httpRequest);
        return ApiResponse.success("讲解点更新成功", locationService.updateLocation(id, request));
    }

    @DeleteMapping("/locations/{id}")
    public ApiResponse<Void> deleteLocation(@PathVariable Long id, HttpServletRequest httpRequest) {
        ensureAdmin(httpRequest);
        locationService.deleteLocation(id);
        return ApiResponse.success("讲解点删除成功", null);
    }

    private UserAccount ensureAdmin(HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        authService.requireAdmin(user);
        return user;
    }
}
