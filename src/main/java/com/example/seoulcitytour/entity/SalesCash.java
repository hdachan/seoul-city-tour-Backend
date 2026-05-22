package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "sales_cash")
@Getter
@NoArgsConstructor
public class SalesCash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String salesUsername;

    @Column(nullable = false)
    private LocalDate date;

    // "수입" or "지출"
    @Column(nullable = false)
    private String type;

    // 수입일 때만: "카드" / "현금" / "기타"
    @Column
    private String paymentType;

    @Column
    private String category;

    // 카테고리 단위 스냅샷 ("원" or "L")
    @Column
    private String unit = "원";

    @Column
    private String content;

    // unit="L" 일 때 주유량
    @Column
    private Double amount;

    @Column
    private Long totalAmount;

    @Column
    private Long supplyAmount;

    @Column
    private Long vat;

    @Column
    private String companyName;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Boolean locked = false;
}
