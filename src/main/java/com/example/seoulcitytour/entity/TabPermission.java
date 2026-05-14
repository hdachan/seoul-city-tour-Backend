package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tab_permission",
       uniqueConstraints = @UniqueConstraint(columnNames = {"role_key", "tab_id"}))
@Getter
@NoArgsConstructor
public class TabPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_key", nullable = false)
    private String roleKey;

    // 탭 ID: record, settlement, ginseng, guide-admin, sales-admin, guide-form, sales, dev, admin
    @Column(name = "tab_id", nullable = false)
    private String tabId;
}
