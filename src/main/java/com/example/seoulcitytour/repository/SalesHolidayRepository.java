package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.SalesHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalesHolidayRepository extends JpaRepository<SalesHoliday, Long> {

    Optional<SalesHoliday> findBySalesUsernameAndDate(String salesUsername, LocalDate date);

    List<SalesHoliday> findBySalesUsernameAndDateBetweenOrderByDateAsc(
            String salesUsername, LocalDate start, LocalDate end);
}
