package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_role")
@Getter
@NoArgsConstructor
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예: ROLE_SALES, ROLE_GUIDE, ROLE_ACCOUNTANT
    @Column(nullable = false, unique = true)
    private String roleKey;

    // 표시명: 영업, 가이드, 회계
    @Column(nullable = false)
    private String displayName;

    // 시스템 기본 역할 여부 (ADMIN/DEV/SALES/GUIDE 는 삭제 불가)
    @Column(nullable = false)
    private Boolean isSystem = false;

    @Column(nullable = false)
    private Boolean active = true;
}
