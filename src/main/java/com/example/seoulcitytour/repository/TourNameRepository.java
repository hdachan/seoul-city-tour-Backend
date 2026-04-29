package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.TourName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourNameRepository extends JpaRepository<TourName, Long> {
    List<TourName> findByActiveTrueOrderByNameAsc();
}
