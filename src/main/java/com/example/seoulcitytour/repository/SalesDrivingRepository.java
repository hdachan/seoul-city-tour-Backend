// ── SalesDrivingRepository.java ──
package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.SalesDriving;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesDrivingRepository extends JpaRepository<SalesDriving, Long> {
    List<SalesDriving> findBySalesUsernameAndYearAndMonthOrderByIdAsc(
            String salesUsername, Integer year, Integer month);
}
