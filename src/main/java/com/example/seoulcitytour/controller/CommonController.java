package com.example.seoulcitytour.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CommonController {

    // 누구나 접근 가능
    @GetMapping("/public/hello")
    public Map<String, String> publicHello() {
        return Map.of("message", "누구나 볼 수 있는 공개 API입니다.");
    }

    // 로그인만 하면 누구나 가능 (역할 확인용)
    @GetMapping("/me")
    public Map<String, Object> myInfo(Authentication auth) {
        return Map.of(
            "username", auth.getName(),
            "roles",    auth.getAuthorities().toString()
        );
    }

    // 관리자 + 개발자 둘 다 가능
    @GetMapping("/system/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public Map<String, String> systemSettings() {
        return Map.of("message", "[ADMIN or DEV] 시스템 설정 페이지");
    }
}
