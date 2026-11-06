package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.GuideIncome;

/**
 * 수입 한 건에서 투어 금액 중 현금 / 카드로 들어온 돈 (통계 · 입력창 합계 공용)
 *  - 옵션은 빼고 투어 금액만 셈. 옵션 돈은 🎟️ 옵션별 통계에서 따로 봄
 *  - 현금 · 그외-현금 → 현금, 카드 · 그외-카드 → 카드, 그외-교차 → 적은 현금 / 카드, 완불 · 그외 → 0
 *  - totalAmount 는 투어 금액 + 옵션 합계라서, 투어 금액 = totalAmount - 그 수입의 옵션금액 합 (optionSum)
 *    (예전 기록은 optionCash · optionCard 가 0 이어도 옵션 줄 금액으로 빼서 맞음)
 */
public final class GuideIncomeMoney {

    private GuideIncomeMoney() {}

    /** 옵션 뺀 투어 금액 */
    public static long tour(GuideIncome i, long optionSum) {
        return n(i.getTotalAmount()) - optionSum;
    }

    public static long cash(GuideIncome i, long optionSum) {
        String pt = i.getPaymentType();
        if ("현금".equals(pt) || "그외-현금".equals(pt)) return tour(i, optionSum);
        if ("그외-교차".equals(pt)) return n(i.getCashAmount());
        return 0L;
    }

    public static long card(GuideIncome i, long optionSum) {
        String pt = i.getPaymentType();
        if ("카드".equals(pt) || "그외-카드".equals(pt)) return tour(i, optionSum);
        if ("그외-교차".equals(pt)) return n(i.getCardAmount());
        return 0L;
    }

    private static long n(Long v) {
        return v != null ? v : 0L;
    }
}
