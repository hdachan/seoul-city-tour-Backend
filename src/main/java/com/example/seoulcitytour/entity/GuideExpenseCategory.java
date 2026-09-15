package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guide_expense_category",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "tour_name_id"}))
@Getter
@NoArgsConstructor
public class GuideExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "tour_name_id")
    private Long tourNameId;

    @Column(nullable = false)
    private Boolean active = true;
}