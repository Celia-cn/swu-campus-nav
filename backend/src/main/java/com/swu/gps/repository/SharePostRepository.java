package com.swu.gps.repository;

import com.swu.gps.model.entity.ShareCategory;
import com.swu.gps.model.entity.SharePost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharePostRepository extends JpaRepository<SharePost, Long> {

    List<SharePost> findByCategoryOrderByCreatedAtDesc(ShareCategory category);

    List<SharePost> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<SharePost> findByUser_IdAndCategoryOrderByCreatedAtDesc(Long userId, ShareCategory category);
}
