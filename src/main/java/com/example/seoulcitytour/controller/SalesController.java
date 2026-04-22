package com.example.seoulcitytour.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@PreAuthorize("hasRole('SALES')")
public class SalesController {

    @GetMapping("/customers")
    public Map<String, String> customers() {
        return Map.of("message", "[SALES] 고객 관리 페이지");
    }

    @GetMapping("/contracts")
    public Map<String, String> contracts() {
        return Map.of("message", "[SALES] 계약 관리 페이지");
    }
}
