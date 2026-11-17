package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GuideExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideExpenseRepository extends JpaRepository<GuideExpense, Long> {
    List<GuideExpense> findByGuideUsernameAndYearAndMonthOrderByDateAsc(
            String guideUsername, Integer year, Integer month);

    List<GuideExpense> findByGuideUsernameAndYearAndMonth(
            String guideUsername, Integer year, Integer month);

    // 한 달치 전체 가이드 (🔎 가이드 상세 통계)
    List<GuideExpense> findByYearAndMonthOrderByDateAscIdAsc(Integer year, Integer month);
}
