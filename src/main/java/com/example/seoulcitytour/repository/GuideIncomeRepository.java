package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GuideIncome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideIncomeRepository extends JpaRepository<GuideIncome, Long> {
    List<GuideIncome> findByGuideUsernameAndYearAndMonthOrderByDateAsc(
            String guideUsername, Integer year, Integer month);

    List<GuideIncome> findByGuideUsernameAndYearAndMonth(
            String guideUsername, Integer year, Integer month);

    // 한 달치 전체 가이드 (🔎 가이드 상세 통계)
    List<GuideIncome> findByYearAndMonthOrderByDateAscIdAsc(Integer year, Integer month);
}
