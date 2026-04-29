package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GuideIncome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideIncomeRepository extends JpaRepository<GuideIncome, Long> {
    List<GuideIncome> findByGuideUsernameAndYearAndMonthOrderByDateAsc(
            String guideUsername, Integer year, Integer month);
}
