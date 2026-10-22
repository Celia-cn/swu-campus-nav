package com.swu.gps.repository;

import com.swu.gps.model.entity.RagCategory;
import com.swu.gps.model.entity.RagDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {

    List<RagDocument> findAllByOrderByUpdatedAtDesc();

    List<RagDocument> findByCategoryOrderByUpdatedAtDesc(RagCategory category);

    @Query("""
            SELECT r FROM RagDocument r
            WHERE r.enabled = true
              AND (LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY r.updatedAt DESC
            """)
    List<RagDocument> searchEnabled(@Param("keyword") String keyword);
}
