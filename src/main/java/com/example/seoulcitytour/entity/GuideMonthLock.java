package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guide_month_lock",
       uniqueConstraints = @UniqueConstraint(columnNames = {"guide_username", "year", "month"}))
@Getter
@NoArgsConstructor
public class GuideMonthLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guide_username", nullable = false)
    private String guideUsername;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Boolean locked = false;
}
