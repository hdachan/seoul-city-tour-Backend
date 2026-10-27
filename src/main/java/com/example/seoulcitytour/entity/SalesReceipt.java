package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "sales_receipt")
@Getter
@NoArgsConstructor
public class SalesReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String salesUsername;

    @Column(nullable = false)
    private LocalDate date;

    @Column
    private String category;

    // 카테고리 단위 스냅샷 ("원" or "L") - 저장 시점 단위 보존
    @Column
    private String unit = "원";

    @Column
    private String content;

    // unit="원" 일 때: 총금액(원)
    // unit="L"  일 때: 주유량(L)
    @Column
    private Double amount;

    // unit="원" 일 때만 계산, "L"이면 null
    @Column
    private Long totalAmount;

    @Column
    private Long supplyAmount;

    @Column
    private Long vat;

    @Column
    private String businessNumber;

    @Column
    private String companyName;

    @Column
    private String note;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Boolean locked = false;
}
