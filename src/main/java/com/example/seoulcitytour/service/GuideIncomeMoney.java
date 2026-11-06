package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.GuideIncome;

/**
 * 수입 한 건에서 현금 / 카드로 들어온 돈 (통계 · 입력창 합계 공용)
 *  - 투어 금액: 현금 · 그외-현금 → 현금, 카드 · 그외-카드 → 카드, 그외-교차 → 적은 현금 / 카드, 완불 · 그외 → 0
 *  - 옵션: 옵션마다 고른 현금 / 카드 / 교차 (optionCash · optionCard)
 *  - totalAmount 는 투어 금액 + 옵션 합계라서, 투어 금액 = totalAmount - optionCash - optionCard
 *    (예전 기록의 현금 · 카드 수입 옵션은 optionCash · optionCard 가 0 이고 총액에 들어 있어 그대로 맞음)
 */
public final class GuideIncomeMoney {

    private GuideIncomeMoney() {}

    public static long cash(GuideIncome i) {
        String pt = i.getPaymentType();
        long optCash = n(i.getOptionCash());
        if ("현금".equals(pt) || "그외-현금".equals(pt)) return base(i) + optCash;
        if ("그외-교차".equals(pt)) return n(i.getCashAmount()) + optCash;
        return optCash;
    }

    public static long card(GuideIncome i) {
        String pt = i.getPaymentType();
        long optCard = n(i.getOptionCard());
        if ("카드".equals(pt) || "그외-카드".equals(pt)) return base(i) + optCard;
        if ("그외-교차".equals(pt)) return n(i.getCardAmount()) + optCard;
        return optCard;
    }

    private static long base(GuideIncome i) {
        return n(i.getTotalAmount()) - n(i.getOptionCash()) - n(i.getOptionCard());
    }

    private static long n(Long v) {
        return v != null ? v : 0L;
    }
}
