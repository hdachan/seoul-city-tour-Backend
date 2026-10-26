package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column
    private String name;

    // 법인카드 번호
    @Column
    private String cardNumber;

    @Column(nullable = false)
    private Boolean active = true;

    // 영업: 처음 시작 미터기 (이전 운행 기록이 없을 때 기준이 되는 값)
    @Column
    private Integer startMeter;
}
