package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GuideMonthLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuideMonthLockRepository extends JpaRepository<GuideMonthLock, Long> {
    Optional<GuideMonthLock> findByGuideUsernameAndYearAndMonth(
            String guideUsername, Integer year, Integer month);
}
