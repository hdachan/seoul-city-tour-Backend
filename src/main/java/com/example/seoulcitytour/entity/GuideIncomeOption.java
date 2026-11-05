package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 수입 한 건에 고른 옵션. 이름을 그대로 저장해서 카테고리에서 옵션을 지워도 기록은 남음 */
@Entity
@Table(name = "guide_income_option", indexes = @Index(columnList = "income_id"))
@Getter
@NoArgsConstructor
public class GuideIncomeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "income_id", nullable = false)
    private Long incomeId;

    @Column(nullable = false)
    private String optionName;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String paymentType;     // 현금 / 카드 (그외는 수입 결제유형 그대로)

    public GuideIncomeOption(Long incomeId, String optionName, Long amount, String paymentType) {
        this.incomeId = incomeId;
        this.optionName = optionName;
        this.amount = amount;
        this.paymentType = paymentType;
    }
}
