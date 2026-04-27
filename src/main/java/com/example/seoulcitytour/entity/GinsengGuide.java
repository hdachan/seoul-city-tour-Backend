package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ginseng_guide")
@Getter
@NoArgsConstructor
public class GinsengGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;    // 가이드 이름

    @Column(nullable = false)
    private Boolean active = true;  // 활성/비활성 (비활성 = 숨김처리)
}
