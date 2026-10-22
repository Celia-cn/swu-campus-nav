package com.swu.gps.repository;

import com.swu.gps.model.entity.FeedbackStatus;
import com.swu.gps.model.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {

    @Query("SELECT f FROM UserFeedback f JOIN FETCH f.user ORDER BY f.createdAt DESC")
    List<UserFeedback> findAllWithUserOrderByCreatedAtDesc();

    @Query("SELECT f FROM UserFeedback f JOIN FETCH f.user WHERE f.id = :id")
    Optional<UserFeedback> findByIdWithUser(@Param("id") Long id);

    long countByStatus(FeedbackStatus status);

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    void deleteByUser_Id(Long userId);
}
