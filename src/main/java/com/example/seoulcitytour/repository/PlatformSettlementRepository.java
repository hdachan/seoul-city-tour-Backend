package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.PlatformSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformSettlementRepository extends JpaRepository<PlatformSettlement, Long> {

    // 특정 플랫폼 + 년월 조회 (upsert용)
    Optional<PlatformSettlement> findByPlatformIdAndYearAndMonth(
            Long platformId, Integer year, Integer month);

    // 년도별 전체 조회
    List<PlatformSettlement> findByYear(Integer year);
}