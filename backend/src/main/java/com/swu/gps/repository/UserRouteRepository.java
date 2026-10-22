package com.swu.gps.repository;

import com.swu.gps.model.entity.UserRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRouteRepository extends JpaRepository<UserRoute, Long> {

    @Query("SELECT r FROM UserRoute r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<UserRoute> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT DISTINCT r FROM UserRoute r LEFT JOIN FETCH r.waypoints WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<UserRoute> findByUserIdWithWaypointsOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT r FROM UserRoute r LEFT JOIN FETCH r.waypoints WHERE r.id = :id")
    Optional<UserRoute> findByIdWithWaypoints(@Param("id") Long id);

    @Query("SELECT r FROM UserRoute r WHERE r.isPublic = true ORDER BY r.likeCount DESC")
    List<UserRoute> findPublicRoutesOrderByLikes(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM UserRoute r WHERE r.isPublic = true AND " +
            "(LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<UserRoute> searchPublicRoutes(@Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);

    long countByUserId(Long userId);
}