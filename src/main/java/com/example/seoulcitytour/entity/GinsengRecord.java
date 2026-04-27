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
    private String guideName;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double count;

    // ── 핵심: 저장 시점의 단가를 같이 저장 ──
    // 나중에 단가가 바뀌어도 이 기록의 정산금액은 변하지 않음
    @Column(nullable = false)
    private Long priceSnapshot;
}
