package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.SalesDriving;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SalesDrivingRepository extends JpaRepository<SalesDriving, Long> {

    // 월별 전체 조회 (날짜순)
    List<SalesDriving> findBySalesUsernameAndYearAndMonthOrderByDateAscIdAsc(
            String salesUsername, Integer year, Integer month);

    // 특정 날짜 조회
    List<SalesDriving> findBySalesUsernameAndDateOrderByIdAsc(
            String salesUsername, LocalDate date);

    // 이전 마지막 미터기값 (자동계산용)
    // 같은 날 이전 행 포함: date < :date 이거나 (date = :date AND id < :id)
    @Query("SELECT d FROM SalesDriving d WHERE d.salesUsername = :username " +
            "AND d.meterReading IS NOT NULL " +
            "AND (d.date < :date OR (d.date = :date AND d.id < :id)) " +
            "ORDER BY d.date DESC, d.id DESC")
    List<SalesDriving> findPrevMeterReadingsById(
            @Param("username") String username,
            @Param("date") LocalDate date,
            @Param("id") Long id);

    // 전일 마지막 미터기 (오늘 제외, 이전 날짜만)
    @Query("SELECT d FROM SalesDriving d WHERE d.salesUsername = :username " +
            "AND d.date < :date AND d.meterReading IS NOT NULL " +
            "ORDER BY d.date DESC, d.id DESC")
    List<SalesDriving> findPrevMeterReadings(
            @Param("username") String username,
            @Param("date") LocalDate date);

    // 용무 자동완성 (중복 제거) - username null이면 전체
    @Query("SELECT DISTINCT d.purpose FROM SalesDriving d " +
            "WHERE (:username IS NULL OR d.salesUsername = :username) AND d.purpose IS NOT NULL AND d.purpose != '' " +
            "ORDER BY d.purpose ASC")
    List<String> findDistinctPurposes(@Param("username") String username);

    // 도착지 자동완성 (중복 제거) - username null이면 전체
    @Query("SELECT DISTINCT d.destination FROM SalesDriving d " +
            "WHERE (:username IS NULL OR d.salesUsername = :username) AND d.destination IS NOT NULL AND d.destination != '' " +
            "ORDER BY d.destination ASC")
    List<String> findDistinctDestinations(@Param("username") String username);

    // 월별 입력된 날짜 목록
    @Query("SELECT DISTINCT d.date FROM SalesDriving d " +
            "WHERE d.salesUsername = :username AND d.year = :year AND d.month = :month")
    List<LocalDate> findEnteredDates(
            @Param("username") String username,
            @Param("year") Integer year,
            @Param("month") Integer month);
}