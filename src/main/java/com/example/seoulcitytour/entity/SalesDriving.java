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

    @Column(nullable = false)
    private LocalDate date;

    // "업무" / "주유" / "휴가"
    @Column(nullable = false)
    private String type = "업무";

    // 도착지
    @Column
    private String destination;

    // 도착 시간 (예: "09:40")
    @Column
    private String arrivalTime;

    // 미터기값 (오늘 계량기 숫자)
    // 운행거리는 프론트에서 미터기 기준으로 계산
    @Column
    private Integer meterReading;

    // 용무 (자유입력)
    @Column
    private String purpose;

    // 주유량 (L) - 주유시만
    @Column
    private Double fuelAmount;

    // 주유금액 (원) - 주유시만
    @Column
    private Long fuelCost;

    // 단가 (원/L) - 주유시만
    @Column
    private Integer fuelUnitPrice;

    // 비고
    @Column
    private String note;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Boolean locked = false;
}