package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sales_driving_note")
@Getter
@NoArgsConstructor
public class SalesDrivingNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driving_id", nullable = false)
    private Long drivingId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}