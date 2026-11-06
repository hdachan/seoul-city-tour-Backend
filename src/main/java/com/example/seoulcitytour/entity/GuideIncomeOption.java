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
    private Long amount;            // 옵션금액 (교차는 현금 + 카드)

    @Column(nullable = false)
    private String paymentType;     // 현금 / 카드 / 교차

    private Long cashAmount;        // 교차일 때 현금 몫

    private Long cardAmount;        // 교차일 때 카드 몫

    public GuideIncomeOption(Long incomeId, String optionName, Long amount, String paymentType) {
        this(incomeId, optionName, amount, paymentType, null, null);
    }

    public GuideIncomeOption(Long incomeId, String optionName, Long amount, String paymentType, Long cashAmount, Long cardAmount) {
        this.incomeId = incomeId;
        this.optionName = optionName;
        this.amount = amount;
        this.paymentType = paymentType;
        this.cashAmount = cashAmount;
        this.cardAmount = cardAmount;
    }

    /** 이 옵션 중 현금으로 받은 돈 */
    public long cashPart() {
        if ("교차".equals(paymentType)) return cashAmount != null ? cashAmount : 0L;
        return "현금".equals(paymentType) ? amount : 0L;
    }

    /** 이 옵션 중 카드로 받은 돈 */
    public long cardPart() {
        if ("교차".equals(paymentType)) return cardAmount != null ? cardAmount : 0L;
        return "카드".equals(paymentType) ? amount : 0L;
    }
}
