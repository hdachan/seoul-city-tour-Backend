package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GuideDailyFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideDailyFeeRepository extends JpaRepository<GuideDailyFee, Long> {
    List<GuideDailyFee> findByGuideUsernameAndYearAndMonthOrderByDateAsc(
            String guideUsername, Integer year, Integer month);

    // 한 달치 전체 가이드 (🔎 가이드 상세 통계)
    List<GuideDailyFee> findByYearAndMonthOrderByDateAscIdAsc(Integer year, Integer month);
}
