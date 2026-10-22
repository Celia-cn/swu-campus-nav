package com.swu.gps.repository;

import com.swu.gps.model.entity.SharePostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharePostLikeRepository extends JpaRepository<SharePostLike, Long> {

    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    Optional<SharePostLike> findByPost_IdAndUser_Id(Long postId, Long userId);

    long countByPost_Id(Long postId);
}
