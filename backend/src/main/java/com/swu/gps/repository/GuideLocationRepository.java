package com.swu.gps.repository;

import com.swu.gps.model.entity.GuideLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideLocationRepository extends JpaRepository<GuideLocation, Long> {

    List<GuideLocation> findAllByOrderByLikeCountDescIdAsc();
}
