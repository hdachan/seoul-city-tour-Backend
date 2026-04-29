package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "guide_income")
@Getter
@NoArgsConstructor
public class GuideIncome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String guideUsername;

    @Column(nullable = false)
    private String tourName;

    @Column
    private String representativeName;

    @Column
    private Long amount;            // 1인 금액

    @Column
    private Integer headcount;      // 인원

    @Column
    private Long totalAmount;       // 합계 (금액 × 인원)

    @Column(nullable = false)
    private String paymentType;     // 현금 / 카드 / 그외

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Boolean locked = false;
}
