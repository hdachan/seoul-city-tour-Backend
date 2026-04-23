package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ginseng_setting")
@Getter
@NoArgsConstructor
public class GinsengSetting {

    @Id
    private Long id = 1L;  // 항상 1개만 존재

    @Column(nullable = false)
    private Long pricePerUnit;  // 인삼 1개 단가 (원)
}
