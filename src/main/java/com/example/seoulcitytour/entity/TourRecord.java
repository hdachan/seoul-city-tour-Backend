package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tour_records")
@Getter
@NoArgsConstructor
public class TourRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;         // 날짜

    @ManyToOne
    @JoinColumn(name = "category1_id")
    private Category category1;     // 내역1

    @ManyToOne
    @JoinColumn(name = "category2_id")
    private Category category2;     // 내역2

    @Column(nullable = false)
    private Integer count;          // 대수

    @Column(nullable = false)
    private Long total;             // 토탈 가격

    @Column
    private String memo;            // 비고
}
