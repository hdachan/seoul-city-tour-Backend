package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.GuideIncome;
import com.example.seoulcitytour.entity.GuideIncomeOption;
import com.example.seoulcitytour.repository.GuideIncomeOptionRepository;
import com.example.seoulcitytour.repository.GuideIncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 수입 옵션 저장 (가이드 정산 · 가이드 정산관리 공용)
 *  - 총액 = 기존 계산 + 옵션금액 합계
 *  - 옵션마다 현금 / 카드 / 교차(현금 + 카드)를 고르고, 현금 몫은 optionCash · 카드 몫은 optionCard 에 담음
 *  - 결제유형을 안 보낸 옵션은 수입 결제유형을 따름 (현금 · 그외-현금 → 현금, 카드 · 그외-카드 → 카드)
 *  - 통계의 현금 / 카드 계산은 GuideIncomeMoney
 */
@Service
@RequiredArgsConstructor
public class GuideIncomeOptionService {

    private static final Set<String> OPTION_PAY = Set.of("현금", "카드", "교차");

    private final GuideIncomeRepository incomeRepository;
    private final GuideIncomeOptionRepository optionRepository;

    /** 수입 저장. 금액 필드를 다 채운 income 을 넘김. body 에 options 가 없으면(수정) 기존 옵션 유지 */
    @Transactional(rollbackFor = Exception.class)
    public void save(GuideIncome income, Map<String, Object> body) throws Exception {
        List<GuideIncomeOption> options = parse(income, body.get("options"));
        long sum = 0, cash = 0, card = 0;
        for (GuideIncomeOption o : options) {
            sum += o.getAmount();
            cash += o.cashPart();
            card += o.cardPart();
        }
        long base = income.getTotalAmount() != null ? income.getTotalAmount() : 0L;
        set(income, "totalAmount", base + sum);
        set(income, "optionCash", cash);
        set(income, "optionCard", card);
        incomeRepository.save(income);

        optionRepository.deleteByIncomeId(income.getId());
        optionRepository.flush();
        for (GuideIncomeOption o : options) {
            optionRepository.save(new GuideIncomeOption(income.getId(), o.getOptionName(), o.getAmount(), o.getPaymentType(),
                    o.getCashAmount(), o.getCardAmount()));
        }
    }

    /** 완불 대표자 여러 명: 사람마다 한 건 + 그 사람 옵션. 전부 저장되거나 전부 안 됨 */
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public void saveWanbul(String guideUsername, Map<String, Object> body) throws Exception {
        List<GuideIncome> incomes = GuideWanbul.build(guideUsername, body);
        List<Map<String, Object>> reps = (List<Map<String, Object>>) body.get("reps");
        for (int i = 0; i < incomes.size(); i++) {
            Map<String, Object> repBody = new LinkedHashMap<>();
            repBody.put("options", reps.get(i).getOrDefault("options", List.of()));
            save(incomes.get(i), repBody);
        }
    }

    @Transactional
    public void delete(Long incomeId) {
        optionRepository.deleteByIncomeId(incomeId);
        incomeRepository.deleteById(incomeId);
    }

    /** 수입 번호별 옵션금액 합 (통계에서 옵션을 빼고 투어 금액만 셀 때) */
    public Map<Long, Long> sumByIncome(List<GuideIncome> incomes) {
        Map<Long, Long> result = new java.util.HashMap<>();
        if (incomes.isEmpty()) return result;
        List<Long> ids = incomes.stream().map(GuideIncome::getId).toList();
        for (GuideIncomeOption o : optionRepository.findByIncomeIdInOrderByIdAsc(ids)) {
            result.merge(o.getIncomeId(), o.getAmount() != null ? o.getAmount() : 0L, Long::sum);
        }
        return result;
    }

    /** 수입 목록의 옵션을 수입 번호별로 */
    public Map<Long, List<Map<String, Object>>> byIncome(List<GuideIncome> incomes) {
        Map<Long, List<Map<String, Object>>> result = new LinkedHashMap<>();
        if (incomes.isEmpty()) return result;
        List<Long> ids = incomes.stream().map(GuideIncome::getId).toList();
        for (GuideIncomeOption o : optionRepository.findByIncomeIdInOrderByIdAsc(ids)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("optionName", o.getOptionName());
            m.put("amount", o.getAmount());
            m.put("paymentType", o.getPaymentType());
            m.put("cashAmount", o.cashPart());
            m.put("cardAmount", o.cardPart());
            result.computeIfAbsent(o.getIncomeId(), k -> new ArrayList<>()).add(m);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<GuideIncomeOption> parse(GuideIncome income, Object raw) {
        String payType = income.getPaymentType();
        List<GuideIncomeOption> list = new ArrayList<>();
        if (raw == null) {
            if (income.getId() == null) return list;
            // 옵션을 안 보낸 수정: 기존 옵션 그대로 유지
            for (GuideIncomeOption o : optionRepository.findByIncomeIdOrderByIdAsc(income.getId())) {
                list.add(new GuideIncomeOption(null, o.getOptionName(), o.getAmount(), o.getPaymentType(),
                        o.getCashAmount(), o.getCardAmount()));
            }
            return list;
        }
        for (Map<String, Object> o : (List<Map<String, Object>>) raw) {
            String name = o.get("optionName") != null ? o.get("optionName").toString().trim() : "";
            if (name.isEmpty()) throw new IllegalArgumentException("옵션을 선택해주세요.");
            String pay = resolvePay(payType, o.get("paymentType") != null ? o.get("paymentType").toString() : "");
            if ("교차".equals(pay)) {
                long cashAmt = num(o.get("cashAmount")), cardAmt = num(o.get("cardAmount"));
                if (cashAmt < 0 || cardAmt < 0 || cashAmt + cardAmt <= 0)
                    throw new IllegalArgumentException("교차 옵션의 현금 / 카드 금액을 입력해주세요.");
                list.add(new GuideIncomeOption(null, name, cashAmt + cardAmt, pay, cashAmt, cardAmt));
            } else {
                long amount = num(o.get("amount"));
                if (amount <= 0) throw new IllegalArgumentException("옵션금액을 입력해주세요.");
                list.add(new GuideIncomeOption(null, name, amount, pay));
            }
        }
        return list;
    }

    /** 옵션이 고른 결제유형. 안 골랐으면 현금 · 카드 수입은 그 결제유형을 따르고, 나머지는 골라야 함 */
    private String resolvePay(String incomePay, String optionPay) {
        if (OPTION_PAY.contains(optionPay)) return optionPay;
        if ("현금".equals(incomePay) || "그외-현금".equals(incomePay)) return "현금";
        if ("카드".equals(incomePay) || "그외-카드".equals(incomePay)) return "카드";
        throw new IllegalArgumentException("옵션마다 현금 / 카드 / 교차를 골라주세요.");
    }

    private static long num(Object v) {
        return (v != null && !v.toString().isBlank()) ? Long.parseLong(v.toString()) : 0L;
    }

    private static void set(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
