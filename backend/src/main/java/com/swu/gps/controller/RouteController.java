package com.swu.gps.controller;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.request.MultiStopRouteRequest;
import com.swu.gps.model.request.RoutePlanRequest;
import com.swu.gps.model.request.SaveRouteRequest;
import com.swu.gps.model.response.ApiResponse;
import com.swu.gps.model.response.RoutePlanResponse;
import com.swu.gps.model.response.UserRouteDetail;
import com.swu.gps.model.response.UserRouteSummary;
import com.swu.gps.service.AuthService;
import com.swu.gps.service.RoutePlanningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private static final Logger log = LoggerFactory.getLogger(RouteController.class);

    private final RoutePlanningService routePlanningService;
    private final AuthService authService;

    public RouteController(RoutePlanningService routePlanningService, AuthService authService) {
        this.routePlanningService = routePlanningService;
        this.authService = authService;
    }

    @PostMapping("/plan")
    public ApiResponse<RoutePlanResponse> planRoute(
            @Valid @RequestBody RoutePlanRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(routePlanningService.planRoute(request));
    }

    @PostMapping("/plan-multi-stop")
    public ApiResponse<RoutePlanResponse> planMultiStopRoute(
            @Valid @RequestBody MultiStopRouteRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(routePlanningService.planMultiStopRoute(request));
    }

    @GetMapping("/my-routes")
    public ApiResponse<List<UserRouteSummary>> getMyRoutes(HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return ApiResponse.success(routePlanningService.getUserRoutes(user.getId()));
    }

    @PostMapping("/save")
    public ApiResponse<UserRouteSummary> saveRoute(
            @Valid @RequestBody SaveRouteRequest request,
            HttpServletRequest httpRequest) {
        try {
            UserAccount user = authService.requireUser(httpRequest);
            log.info("保存路线请求: 用户={}, 路线名称={}, 途经点数量={}",
                    user.getUsername(), request.name(), request.waypoints().size());

            UserRouteSummary result = routePlanningService.saveRoute(user, request);

            log.info("路线保存成功: ID={}, 名称={}", result.id(), result.name());
            return ApiResponse.success("路线保存成功", result);
        } catch (Exception e) {
            log.error("保存路线失败", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "保存路线失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<UserRouteDetail> getRouteDetail(
            @PathVariable Long id,
            HttpServletRequest request) {
        return ApiResponse.success(routePlanningService.getRouteDetail(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRoute(
            @PathVariable Long id,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        routePlanningService.deleteRoute(user, id);
        return ApiResponse.success("路线已删除", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        log.warn("路线保存参数验证失败: {}", errors);
        return ApiResponse.fail(HttpStatus.BAD_REQUEST.value(),
                String.join("; ", errors));
    }
}