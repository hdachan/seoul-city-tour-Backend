package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.TourRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TourRecordRepository extends JpaRepository<TourRecord, Long> {

    List<TourRecord> findAllByOrderByDateDesc();

    // 특정 년도 데이터 조회
    List<TourRecord> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);

    // 특정 년월 데이터 조회
    @Query("SELECT r FROM TourRecord r WHERE YEAR(r.date) = :year AND MONTH(r.date) = :month")
    List<TourRecord> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
}
