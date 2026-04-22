package com.example.seoulcitytour.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@PreAuthorize("hasRole('DEV')")
public class DevController {

    @GetMapping("/api-list")
    public Map<String, String> apiList() {
        return Map.of("message", "[DEV] API 목록 관리 페이지");
    }

    @GetMapping("/monitor")
    public Map<String, String> monitor() {
        return Map.of("message", "[DEV] 서버 모니터링 페이지");
    }
}
