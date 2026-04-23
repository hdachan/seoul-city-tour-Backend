package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "platform_settlement",
       uniqueConstraints = @UniqueConstraint(columnNames = {"platform_id", "year", "month", "region"}))
@Getter
@NoArgsConstructor
public class PlatformSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "platform_id", nullable = false)
    private Platform platform;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String region;  // "국내" or "해외"

    @Column
    private String memo;    // 비고
}
