package com.swu.gps.repository;

import com.swu.gps.model.entity.SharePostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharePostCommentRepository extends JpaRepository<SharePostComment, Long> {

    List<SharePostComment> findByPost_IdOrderByCreatedAtAsc(Long postId);
}
