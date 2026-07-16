package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.SalesDailyNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalesDailyNoteRepository extends JpaRepository<SalesDailyNote, Long> {

    Optional<SalesDailyNote> findBySalesUsernameAndDate(String salesUsername, LocalDate date);

    List<SalesDailyNote> findBySalesUsernameAndDateBetween(
            String salesUsername, LocalDate start, LocalDate end);
}