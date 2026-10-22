package com.swu.gps.controller;

import com.swu.gps.model.request.AiChatRequest;
import com.swu.gps.model.response.AiChatResponse;
import com.swu.gps.model.response.ApiResponse;
import com.swu.gps.service.AiChatService;
import com.swu.gps.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AuthService authService;
    private final AiChatService aiChatService;

    public AiController(AuthService authService, AiChatService aiChatService) {
        this.authService = authService;
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request,
                                            HttpServletRequest httpRequest) {
        authService.requireUser(httpRequest);
        return ApiResponse.success(aiChatService.chat(request));
    }
}
