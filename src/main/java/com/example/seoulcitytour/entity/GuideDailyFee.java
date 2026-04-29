package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "guide_daily_fee")
@Getter
@NoArgsConstructor
public class GuideDailyFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String guideUsername;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private LocalDate date;         // 사용자가 직접 입력

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Boolean locked = false;
}
