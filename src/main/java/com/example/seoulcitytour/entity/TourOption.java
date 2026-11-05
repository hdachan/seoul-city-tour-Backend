package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 투어 카테고리 밑의 옵션 (금액은 수입 입력할 때 직접 적음) */
@Entity
@Table(name = "tour_option",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "tour_name_id"}))
@Getter
@NoArgsConstructor
public class TourOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "tour_name_id", nullable = false)
    private Long tourNameId;

    public TourOption(String name, Long tourNameId) {
        this.name = name;
        this.tourNameId = tourNameId;
    }
}
