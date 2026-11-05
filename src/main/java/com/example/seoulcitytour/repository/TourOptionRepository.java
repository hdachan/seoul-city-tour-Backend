package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.TourOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TourOptionRepository extends JpaRepository<TourOption, Long> {
    List<TourOption> findByTourNameIdOrderByNameAsc(Long tourNameId);
    boolean existsByNameAndTourNameId(String name, Long tourNameId);

    @Transactional
    void deleteByTourNameId(Long tourNameId);
}
