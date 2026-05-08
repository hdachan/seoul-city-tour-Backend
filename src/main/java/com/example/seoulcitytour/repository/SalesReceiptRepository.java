// ── SalesReceiptRepository.java ──
package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.SalesReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesReceiptRepository extends JpaRepository<SalesReceipt, Long> {
    List<SalesReceipt> findBySalesUsernameAndYearAndMonthOrderByDateAsc(
            String salesUsername, Integer year, Integer month);
}
