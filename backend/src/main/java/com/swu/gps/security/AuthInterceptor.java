package com.swu.gps.security;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.UserSession;
import com.swu.gps.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER = "CURRENT_USER";

    private final UserSessionRepository userSessionRepository;

    public AuthInterceptor(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublicEndpoint(request)) {
            return true;
        }

        String token = extractToken(request.getHeader("Authorization"));
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录或登录凭证缺失");
        }

        UserSession session = userSessionRepository
                .findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录"));

        request.setAttribute(CURRENT_USER, session.getUser());
        return true;
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (uri.equals("/api/auth/login")
                || uri.equals("/api/auth/register")
                || uri.equals("/api/auth/admin/register")
                || uri.equals("/api/auth/reset-password")
                || uri.equals("/api/health")) {
            return true;
        }

        if ("GET".equalsIgnoreCase(method) && uri.startsWith("/media/")) {
            return true;
        }

        return "GET".equalsIgnoreCase(method) && uri.startsWith("/api/locations");
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        String prefix = "Bearer ";
        if (authHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return authHeader.substring(prefix.length()).trim();
        }
        return authHeader.trim();
    }
}
