// ── SalesMonthLockRepository.java ──
package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.SalesMonthLock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SalesMonthLockRepository extends JpaRepository<SalesMonthLock, Long> {
    Optional<SalesMonthLock> findBySalesUsernameAndYearAndMonth(
            String salesUsername, Integer year, Integer month);
}
