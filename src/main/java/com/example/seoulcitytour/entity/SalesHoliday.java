package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

// 🏖 영업 휴무: 영업 사원이 쉬는 날과 사유 (하루 한 줄)
@Entity
@Table(name = "sales_holiday",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sales_username", "date"}))
@Getter
@Setter
@NoArgsConstructor
public class SalesHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String salesUsername;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 500)
    private String reason;
}
