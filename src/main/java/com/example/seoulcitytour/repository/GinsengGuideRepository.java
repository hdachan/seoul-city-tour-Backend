package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GinsengGuide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GinsengGuideRepository extends JpaRepository<GinsengGuide, Long> {
    List<GinsengGuide> findByActiveTrueOrderByNameAsc();   // 활성 가이드만
    List<GinsengGuide> findAllByOrderByActiveDescNameAsc(); // 전체 (비활성 포함)
}
