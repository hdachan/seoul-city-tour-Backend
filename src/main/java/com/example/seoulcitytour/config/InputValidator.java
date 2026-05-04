package com.example.seoulcitytour.config;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 입력값 검증 유틸리티
 * SQL Injection 방어 + 기본 유효성 검사
 */
@Component
public class InputValidator {

    // SQL Injection 위험 패턴
    private static final Pattern SQL_PATTERN = Pattern.compile(
        "('|--|;|/\\*|\\*/|xp_|UNION|SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|CAST|CONVERT|CHAR|NCHAR|VARCHAR|NVARCHAR|ALTER|BEGIN|CURSOR|DECLARE|END|GOTO|TABLE|DATABASE)",
        Pattern.CASE_INSENSITIVE
    );

    // 아이디: 영문+숫자만, 4~20자
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4,20}$");

    // 이름: 한글+영문+공백, 2~20자
    private static final Pattern NAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z\\s]{2,20}$");

    // 일반 텍스트: 특수문자 최소화
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9\\s\\-_.,()]{1,100}$");

    // 금액: 숫자만
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d{1,15}$");

    public boolean isSqlInjection(String input) {
        if (input == null) return false;
        return SQL_PATTERN.matcher(input).find();
    }

    public boolean isValidUsername(String username) {
        if (username == null) return false;
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public boolean isValidName(String name) {
        if (name == null || name.isBlank()) return true; // 선택 필드
        return NAME_PATTERN.matcher(name.trim()).matches();
    }

    public boolean isValidSafeText(String text) {
        if (text == null || text.isBlank()) return true;
        return SAFE_TEXT_PATTERN.matcher(text.trim()).matches();
    }

    public boolean isValidAmount(String amount) {
        if (amount == null || amount.isBlank()) return true;
        return AMOUNT_PATTERN.matcher(amount.trim()).matches();
    }

    // 입력값에서 위험 문자 제거 (최후 수단)
    public String sanitize(String input) {
        if (input == null) return null;
        return input
            .replaceAll("['\";\\-\\-]", "")
            .trim();
    }

    // 종합 검사 - SQL 인젝션 시도 여부
    public void validateSafe(String fieldName, String value) {
        if (isSqlInjection(value)) {
            throw new IllegalArgumentException("유효하지 않은 입력값입니다: " + fieldName);
        }
    }
}
