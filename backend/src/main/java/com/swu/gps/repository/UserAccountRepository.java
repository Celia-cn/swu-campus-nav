package com.swu.gps.repository;

import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRole(UserRole role);

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);
}
