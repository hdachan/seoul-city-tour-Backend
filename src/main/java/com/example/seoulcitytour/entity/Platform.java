package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "platform")
@Getter
@NoArgsConstructor
public class Platform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String region;   // 국내 / 해외

    @Column
    private String memo;     // 기본 비고 (매월 불러옴)

    @Column(nullable = false)
    private Boolean active = true;
}
