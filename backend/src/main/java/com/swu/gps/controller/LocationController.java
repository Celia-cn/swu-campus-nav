package com.swu.gps.controller;

import com.swu.gps.model.entity.GuideLocation;
import com.swu.gps.model.entity.TriggerType;
import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.request.CheckInRequest;
import com.swu.gps.model.request.ManualPlayRequest;
import com.swu.gps.model.response.ApiResponse;
import com.swu.gps.model.response.CheckInResponse;
import com.swu.gps.model.response.LocationLikeResponse;
import com.swu.gps.model.response.LocationResponse;
import com.swu.gps.model.response.TriggerHistoryResponse;
import com.swu.gps.model.response.TriggeredLocationResponse;
import com.swu.gps.repository.GuideLocationRepository;
import com.swu.gps.service.AuthService;
import com.swu.gps.service.LocationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;
    private final AuthService authService;
    private final GuideLocationRepository guideLocationRepository;

    public LocationController(LocationService locationService, AuthService authService, GuideLocationRepository guideLocationRepository) {
        this.locationService = locationService;
        this.authService = authService;
        this.guideLocationRepository = guideLocationRepository;
    }

    @GetMapping
    public ApiResponse<List<LocationResponse>> list(HttpServletRequest request) {
        Long userId = authService.findUserOptional(request).map(UserAccount::getId).orElse(null);
        return ApiResponse.success(locationService.listLocations(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<LocationResponse> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = authService.findUserOptional(request).map(UserAccount::getId).orElse(null);
        return ApiResponse.success(locationService.getLocation(id, userId));
    }

    @GetMapping("/{id}/trigger-history")
    public ApiResponse<List<TriggerHistoryResponse>> getTriggerHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return ApiResponse.success(locationService.getUserTriggerHistory(user, id, limit));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<LocationLikeResponse> toggleLike(@PathVariable Long id, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return ApiResponse.success(locationService.toggleLike(user, id));
    }

    @PostMapping("/check-in")
    public ApiResponse<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request,
                                                HttpServletRequest httpRequest) {
        UserAccount user = authService.requireUser(httpRequest);

        CheckInResponse response = locationService.checkIn(user, request);

        if (response.triggeredLocations() != null) {
            for (TriggeredLocationResponse triggered : response.triggeredLocations()) {
                GuideLocation location = guideLocationRepository.findById(triggered.locationId()).orElse(null);
                if (location != null) {
                    LocationService.TriggerCheckResult checkResult = locationService.shouldTriggerExplanation(
                            user, location, request.latitude(), request.longitude(),
                            triggered.distanceMeters());

                    locationService.recordTriggerHistory(
                            user, location, TriggerType.AUTO,
                            request.latitude(), request.longitude(),
                            triggered.distanceMeters(),
                            checkResult.shouldTrigger(),
                            checkResult.getReason(),
                            null
                    );
                }
            }
        }

        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/play")
    public ApiResponse<Void> manualPlay(@PathVariable Long id,
                                        @RequestBody(required = false) ManualPlayRequest request,
                                        HttpServletRequest httpRequest) {
        UserAccount user = authService.requireUser(httpRequest);
        locationService.recordManualPlay(user, id, request);
        return ApiResponse.success("播放记录已保存", null);
    }
}
