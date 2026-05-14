package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GinsengGuide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GinsengGuideRepository extends JpaRepository<GinsengGuide, Long> {
    List<GinsengGuide> findByActiveTrueOrderByNameAsc();
    List<GinsengGuide> findAllByOrderByActiveDescNameAsc();
    boolean existsByName(String name);  // ← 이것만 추가
}