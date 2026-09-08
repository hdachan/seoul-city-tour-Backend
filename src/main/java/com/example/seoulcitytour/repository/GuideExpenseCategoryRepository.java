package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GuideExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuideExpenseCategoryRepository extends JpaRepository<GuideExpenseCategory, Long> {
    List<GuideExpenseCategory> findByActiveTrueOrderByNameAsc();
    boolean existsByName(String name);
}