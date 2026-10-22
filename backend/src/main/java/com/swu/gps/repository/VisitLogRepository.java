package com.swu.gps.repository;

import com.swu.gps.model.entity.TriggerType;
import com.swu.gps.model.entity.VisitLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    long countByCreatedAtAfter(LocalDateTime after);

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    @Query("select count(distinct v.user.id) from VisitLog v where v.createdAt >= :after")
    long countDistinctUsersAfter(@Param("after") LocalDateTime after);

    long countByTriggerType(TriggerType triggerType);

    long countByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);

    long countByUser_IdAndTriggerType(Long userId, TriggerType triggerType);

    @Query("select count(distinct v.location.id) from VisitLog v where v.user.id = :userId")
    long countDistinctLocationsByUserId(@Param("userId") Long userId);

    @Query("select v.location.name as locationName, max(v.createdAt) as visitedAt "
            + "from VisitLog v where v.user.id = :userId "
            + "group by v.location.id, v.location.name "
            + "order by max(v.createdAt) desc")
    List<RecentVisitProjection> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("select v.location.id as locationId, v.location.name as locationName, count(v) as playCount "
            + "from VisitLog v "
            + "group by v.location.id, v.location.name "
            + "order by count(v) desc")
    List<HotLocationProjection> findHotLocations(Pageable pageable);

    @Query("select v.location.id as locationId, count(v) as playCount "
            + "from VisitLog v "
            + "group by v.location.id")
    List<LocationPlayCountProjection> findPlayCountGroupByLocation();

    @Query(value = """
            SELECT HOUR(created_at) AS hourSlot, COUNT(*) AS visitCount
            FROM visit_log
            GROUP BY HOUR(created_at)
            ORDER BY hourSlot
            """, nativeQuery = true)
    List<VisitHourProjection> countVisitsByHour();

    interface VisitHourProjection {
        Integer getHourSlot();

        Long getVisitCount();
    }

    interface LocationPlayCountProjection {
        Long getLocationId();

        Long getPlayCount();
    }

    interface HotLocationProjection {
        Long getLocationId();

        String getLocationName();

        Long getPlayCount();
    }

    interface RecentVisitProjection {
        String getLocationName();

        LocalDateTime getVisitedAt();
    }
}
