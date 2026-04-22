package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer type;   // 1 = 내역1, 2 = 내역2

    @Column(nullable = false)
    private String name;    // 예: 버스, 카운티

    @Column(nullable = false)
    private Long price;     // 단가
}
