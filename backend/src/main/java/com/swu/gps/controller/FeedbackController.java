package com.swu.gps.controller;

import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.request.FeedbackSubmitRequest;
import com.swu.gps.model.response.ApiResponse;
import com.swu.gps.model.response.FeedbackResponse;
import com.swu.gps.service.AuthService;
import com.swu.gps.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final AuthService authService;
    private final FeedbackService feedbackService;

    public FeedbackController(AuthService authService, FeedbackService feedbackService) {
        this.authService = authService;
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<FeedbackResponse> submit(@Valid @RequestBody FeedbackSubmitRequest request,
                                                HttpServletRequest httpRequest) {
        UserAccount user = authService.requireUser(httpRequest);
        return ApiResponse.success("反馈已提交", feedbackService.submit(user, request));
    }
}
