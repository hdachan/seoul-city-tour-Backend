package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.SalesCash;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesCashRepository extends JpaRepository<SalesCash, Long> {
    List<SalesCash> findBySalesUsernameAndYearAndMonthOrderByDateAsc(
            String salesUsername, Integer year, Integer month);
}
