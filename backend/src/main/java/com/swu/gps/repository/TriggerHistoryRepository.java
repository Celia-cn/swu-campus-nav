package com.swu.gps.repository;

import com.swu.gps.model.entity.TriggerHistory;
import com.swu.gps.model.entity.TriggerType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TriggerHistoryRepository extends JpaRepository<TriggerHistory, Long> {

    // 查询用户的触发历史
    List<TriggerHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 查询用户在某讲解点的触发历史
    List<TriggerHistory> findByUserIdAndLocationIdOrderByCreatedAtDesc(
            Long userId, Long locationId, Pageable pageable);

    // 查询某个讲解点的触发历史
    List<TriggerHistory> findByLocationIdOrderByCreatedAtDesc(Long locationId, Pageable pageable);

    // 查询用户某段时间内的触发次数
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    // 查询用户某个讲解点的触发次数
    long countByUserIdAndLocationId(Long userId, Long locationId);

    // 查询用户某个讲解点最近的触发时间
    @Query("SELECT MAX(th.createdAt) FROM TriggerHistory th WHERE th.user.id = :userId AND th.location.id = :locationId")
    LocalDateTime findLastTriggerTime(@Param("userId") Long userId, @Param("locationId") Long locationId);

    // 统计各种触发类型的数量
    long countByUserIdAndTriggerType(Long userId, TriggerType triggerType);

    // 查询触发成功率
    @Query("SELECT COUNT(th), SUM(CASE WHEN th.success = true THEN 1 ELSE 0 END) FROM TriggerHistory th WHERE th.user.id = :userId")
    Object[] countSuccessAndTotalByUserId(@Param("userId") Long userId);
}