package com.example.seoulcitytour.service;

/**
 * 가이드 수입 총액 계산 (가이드 정산 · 가이드 정산관리 공용)
 *  - 보통 투어: 1인 금액 × 어른 + 아이 금액 × 아이
 *  - 프라이빗투어: 적은 금액이 곧 총액 (인원은 기록만, 곱하지 않음)
 *    → 총액을 적고 인원을 적으면 두 번 곱해지던 문제 방지
 */
public final class GuideIncomeCalc {

    private GuideIncomeCalc() {}

    public static boolean isPrivate(String tourName) {
        return tourName != null && tourName.contains("프라이빗");
    }

    public static long total(String tourName, long amount, int adult, long childAmount, int child) {
        if (isPrivate(tourName)) return amount;
        return amount * adult + childAmount * child;
    }
}
