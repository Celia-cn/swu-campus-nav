package com.swu.gps.repository;

import com.swu.gps.model.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByTokenAndExpiresAtAfter(String token, LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime now);

    void deleteByUser_Id(Long userId);
}
