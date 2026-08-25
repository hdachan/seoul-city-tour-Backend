package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.SalesDrivingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SalesDrivingNoteRepository extends JpaRepository<SalesDrivingNote, Long> {
    List<SalesDrivingNote> findByDrivingIdOrderBySortOrderAsc(Long drivingId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SalesDrivingNote n WHERE n.drivingId = :drivingId")
    void deleteByDrivingId(Long drivingId);
}