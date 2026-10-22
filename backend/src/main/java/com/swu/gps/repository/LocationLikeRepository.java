package com.swu.gps.repository;

import com.swu.gps.model.entity.LocationLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LocationLikeRepository extends JpaRepository<LocationLike, Long> {

    boolean existsByUser_IdAndLocation_Id(Long userId, Long locationId);

    Optional<LocationLike> findByUser_IdAndLocation_Id(Long userId, Long locationId);

    void deleteByUser_IdAndLocation_Id(Long userId, Long locationId);

    @Query("SELECT ll.location.id FROM LocationLike ll WHERE ll.user.id = :userId AND ll.location.id IN :locationIds")
    List<Long> findLikedLocationIds(@Param("userId") Long userId, @Param("locationIds") Collection<Long> locationIds);
}
