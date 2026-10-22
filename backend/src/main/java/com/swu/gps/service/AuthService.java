package com.swu.gps.service;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.entity.UserRole;
import com.swu.gps.model.entity.UserSession;
import com.swu.gps.model.request.LoginRequest;
import com.swu.gps.model.request.RegisterRequest;
import com.swu.gps.model.request.ResetPasswordRequest;
import com.swu.gps.model.response.AuthResponse;
import com.swu.gps.model.response.UserProfileResponse;
import com.swu.gps.repository.UserAccountRepository;
import com.swu.gps.repository.UserSessionRepository;
import com.swu.gps.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.session-days:7}")
    private long sessionDays;

    public AuthService(UserAccountRepository userAccountRepository,
                       UserSessionRepository userSessionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {
        return register(request, UserRole.USER);
    }

    @Transactional
    public AuthResponse registerAdmin(RegisterRequest request) {
        return register(request, UserRole.ADMIN);
    }

    private AuthResponse register(RegisterRequest request, UserRole role) {
        String username = normalizeUsername(request.username());
        if (userAccountRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setCreatedAt(now);
        userAccountRepository.save(user);

        return createSessionResponse(user, now);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String username = normalizeUsername(request.username());
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccountRepository.save(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.username());
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        if (isWebClient(request.client()) && user.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "普通用户无法登录管理后台，请使用手机 App");
        }

        return createSessionResponse(user, LocalDateTime.now());
    }

    public UserAccount requireUser(HttpServletRequest request) {
        return findUserOptional(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "未登录"));
    }

    public Optional<UserAccount> findUserOptional(HttpServletRequest request) {
        Object currentUser = request.getAttribute(AuthInterceptor.CURRENT_USER);
        if (currentUser instanceof UserAccount user) {
            return Optional.of(user);
        }
        String token = extractBearerToken(request.getHeader("Authorization"));
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        return userSessionRepository.findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                .map(UserSession::getUser);
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return null;
        }
        String prefix = "Bearer ";
        if (authHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return authHeader.substring(prefix.length()).trim();
        }
        return authHeader.trim();
    }

    public void requireAdmin(UserAccount user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "需要管理员权限");
        }
    }

    public UserProfileResponse toUserProfile(UserAccount user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getRole().name(), user.getCreatedAt());
    }

    private AuthResponse createSessionResponse(UserAccount user, LocalDateTime now) {
        userSessionRepository.deleteByExpiresAtBefore(now);

        UserSession session = new UserSession();
        session.setToken(UUID.randomUUID().toString().replace("-", ""));
        session.setUser(user);
        session.setCreatedAt(now);
        session.setExpiresAt(now.plusDays(sessionDays));
        userSessionRepository.save(session);

        return new AuthResponse(session.getToken(), session.getExpiresAt(), toUserProfile(user));
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private boolean isWebClient(String client) {
        if (client == null || client.trim().isEmpty()) {
            return false;
        }
        return "WEB".equalsIgnoreCase(client.trim());
    }
}
