package com.example.seoulcitytour.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/guide")
@PreAuthorize("hasRole('GUIDE')")
public class GuideController {

    @GetMapping("/content")
    public Map<String, String> content() {
        return Map.of("message", "[GUIDE] 콘텐츠 관리 페이지");
    }

    @GetMapping("/faq")
    public Map<String, String> faq() {
        return Map.of("message", "[GUIDE] FAQ 관리 페이지");
    }
}
