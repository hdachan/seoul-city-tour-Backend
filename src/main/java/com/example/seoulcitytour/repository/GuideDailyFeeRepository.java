package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GuideDailyFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideDailyFeeRepository extends JpaRepository<GuideDailyFee, Long> {
    List<GuideDailyFee> findByGuideUsernameAndYearAndMonthOrderByDateAsc(
            String guideUsername, Integer year, Integer month);
}
