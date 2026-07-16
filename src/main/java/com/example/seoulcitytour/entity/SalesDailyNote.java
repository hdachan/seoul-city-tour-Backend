package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "sales_daily_note",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sales_username", "date"}))
@Getter
@NoArgsConstructor
public class SalesDailyNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String salesUsername;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 500)
    private String note;
}