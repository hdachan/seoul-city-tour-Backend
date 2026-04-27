package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GinsengRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GinsengRecordRepository extends JpaRepository<GinsengRecord, Long> {
    List<GinsengRecord> findByDateBetweenOrderByGuideNameAsc(LocalDate start, LocalDate end);
    Optional<GinsengRecord> findByGuideNameAndDate(String guideName, LocalDate date);
}
