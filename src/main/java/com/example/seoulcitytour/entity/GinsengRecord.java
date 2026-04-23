package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "ginseng_record",
       uniqueConstraints = @UniqueConstraint(columnNames = {"guide_name", "date"}))
@Getter
@NoArgsConstructor
public class GinsengRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guide_name", nullable = false)
    private String guideName;   // 가이드 이름

    @Column(nullable = false)
    private LocalDate date;     // 날짜

    @Column(nullable = false)
    private Double count;       // 인삼 갯수 (소수점 가능)
}
