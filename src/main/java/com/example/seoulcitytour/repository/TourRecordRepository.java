package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.TourRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourRecordRepository extends JpaRepository<TourRecord, Long> {
    List<TourRecord> findAllByOrderByDateDesc();
}
