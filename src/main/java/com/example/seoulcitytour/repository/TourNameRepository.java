package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.TourName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TourNameRepository extends JpaRepository<TourName, Long> {
    List<TourName> findByActiveTrueOrderByNameAsc();
    boolean existsByName(String name);
    Optional<TourName> findByName(String name);
}