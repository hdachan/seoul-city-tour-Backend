package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sales_month_lock",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sales_username", "year", "month", "week_num"}))
@Getter
@NoArgsConstructor
public class SalesMonthLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_username", nullable = false)
    private String salesUsername;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    // 0 = 월 전체 잠금, 1~5 = 주 단위 잠금
    @Column(name = "week_num", nullable = false)
    private Integer weekNum = 0;

    @Column(nullable = false)
    private Boolean locked = false;

    // 관리자가 직접 열거나 잠근 주 (true) → 자정 · 서버 재시작 자동 잠금이 건드리지 않음
    //   null / false = 자동으로 만든 기록 (이번 주 자동 오픈 등)
    @Column(name = "admin_set")
    private Boolean adminSet;
}