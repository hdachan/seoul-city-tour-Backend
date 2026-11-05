package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.GuideIncomeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GuideIncomeOptionRepository extends JpaRepository<GuideIncomeOption, Long> {
    List<GuideIncomeOption> findByIncomeIdOrderByIdAsc(Long incomeId);
    List<GuideIncomeOption> findByIncomeIdInOrderByIdAsc(Collection<Long> incomeIds);
    void deleteByIncomeId(Long incomeId);
}
