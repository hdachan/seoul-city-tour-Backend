package com.example.seoulcitytour.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 요청사항 (관리자 → 영업·가이드)
 *  - 받는 사람이 접속하면 팝업으로 뜸
 *  - status: SENT(안 읽음) → READ(확인) → DONE(수정 완료)
 */
@Entity
@Table(name = "user_request")
@Getter
@NoArgsConstructor
public class UserRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fromUsername;

    @Column(nullable = false)
    private String toUsername;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private String status = "SENT";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime readAt;

    @Column
    private LocalDateTime doneAt;
}
