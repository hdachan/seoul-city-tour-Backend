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
    private LocalDate date;          // 날짜

    @Column
    private String content;          // 내용

    @Column
    private Long totalAmount;        // 총금액

    @Column
    private Long supplyAmount;       // 공급가액

    @Column
    private String businessNumber;   // 사업자등록번호

    @Column
    private String companyName;      // 상호

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Boolean locked = false;
}
