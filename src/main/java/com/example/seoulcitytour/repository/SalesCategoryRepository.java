package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.SalesCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesCategoryRepository extends JpaRepository<SalesCategory, Long> {
    List<SalesCategory> findByActiveTrueOrderByNameAsc();
    List<SalesCategory> findAllByOrderByNameAsc();
    boolean existsByName(String name);
}
