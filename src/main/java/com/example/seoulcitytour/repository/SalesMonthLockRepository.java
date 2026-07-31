package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.SalesMonthLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesMonthLockRepository extends JpaRepository<SalesMonthLock, Long> {

    // 월 전체 잠금 조회 (weekNum = 0)
    Optional<SalesMonthLock> findBySalesUsernameAndYearAndMonthAndWeekNum(
            String salesUsername, Integer year, Integer month, Integer weekNum);

    // 특정 달의 모든 잠금 조회 (월 + 주 단위)
    List<SalesMonthLock> findBySalesUsernameAndYearAndMonth(
            String salesUsername, Integer year, Integer month);

    // 전체 영업사원의 특정 달 잠금 조회 (스케줄러용)
    List<SalesMonthLock> findByYearAndMonthAndWeekNum(
            Integer year, Integer month, Integer weekNum);
}