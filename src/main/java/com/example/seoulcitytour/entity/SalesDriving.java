package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "sales_driving")
@Getter
@NoArgsConstructor
public class SalesDriving {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String salesUsername;

    @Column
    private String drivingMonth;     // 자동 세팅 (예: "5월")

    @Column
    private LocalDate date;          // 날짜 (이번 달만)

    @Column
    private String totalFuelDetail;  // 총주유내역

    @Column
    private Double averageDistance;  // 평균거리 (km)

    @Column
    private Long totalFuelCost;      // 총주유금액

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Boolean locked = false;
}
