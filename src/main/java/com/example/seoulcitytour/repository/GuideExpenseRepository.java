package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GuideExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideExpenseRepository extends JpaRepository<GuideExpense, Long> {
    List<GuideExpense> findByGuideUsernameAndYearAndMonthOrderByDateAsc(
            String guideUsername, Integer year, Integer month);
}
