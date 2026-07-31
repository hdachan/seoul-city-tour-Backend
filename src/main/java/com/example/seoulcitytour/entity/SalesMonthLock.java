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
}