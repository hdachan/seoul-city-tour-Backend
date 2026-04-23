package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.PlatformSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformSettlementRepository extends JpaRepository<PlatformSettlement, Long> {

    // 특정 년월 전체 조회
    List<PlatformSettlement> findByYearAndMonthOrderByPlatformNameAsc(Integer year, Integer month);

    // 특정 플랫폼 + 년월 + 지역 조회 (중복 방지)
    Optional<PlatformSettlement> findByPlatformIdAndYearAndMonthAndRegion(
            Long platformId, Integer year, Integer month, String region);

    // 연간 조회
    List<PlatformSettlement> findByYearOrderByMonthAscPlatformNameAsc(Integer year);
}
