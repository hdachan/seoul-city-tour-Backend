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
 *  - 현금 / 카드 수입의 옵션은 그 결제유형으로 총액에 포함되어 기존 합계에 잡힘
 *  - 완불 / 그외 / 그외-교차 는 결제유형으로 합계에 안 잡히므로
 *    옵션마다 고른 현금 / 카드를 optionCash / optionCard 에 따로 담고, 합계 계산에서 더함
 */
@Service
@RequiredArgsConstructor
public class GuideIncomeOptionService {

    private static final Set<String> CHOOSE_PAY = Set.of("완불", "그외", "그외-교차");

    private final GuideIncomeRepository incomeRepository;
    private final GuideIncomeOptionRepository optionRepository;

    /** 수입 저장. 금액 필드를 다 채운 income 을 넘김. body 에 options 가 없으면(수정) 기존 옵션 유지 */
    @Transactional(rollbackFor = Exception.class)
    public void save(GuideIncome income, Map<String, Object> body) throws Exception {
        List<GuideIncomeOption> options = parse(income, body.get("options"));
        long sum = 0, cash = 0, card = 0;
        boolean choose = CHOOSE_PAY.contains(income.getPaymentType());
        for (GuideIncomeOption o : options) {
            sum += o.getAmount();
            if (choose && "현금".equals(o.getPaymentType())) cash += o.getAmount();
            if (choose && "카드".equals(o.getPaymentType())) card += o.getAmount();
        }
        long base = income.getTotalAmount() != null ? income.getTotalAmount() : 0L;
        set(income, "totalAmount", base + sum);
        set(income, "optionCash", cash);
        set(income, "optionCard", card);
        incomeRepository.save(income);

        optionRepository.deleteByIncomeId(income.getId());
        optionRepository.flush();
        for (GuideIncomeOption o : options) {
            optionRepository.save(new GuideIncomeOption(income.getId(), o.getOptionName(), o.getAmount(), o.getPaymentType()));
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
            // 옵션을 안 보낸 수정: 기존 옵션 유지 (결제유형이 바뀌었으면 맞춰서 다시 계산)
            for (GuideIncomeOption o : optionRepository.findByIncomeIdOrderByIdAsc(income.getId())) {
                list.add(new GuideIncomeOption(null, o.getOptionName(), o.getAmount(), resolvePay(payType, o.getPaymentType())));
            }
            return list;
        }
        for (Map<String, Object> o : (List<Map<String, Object>>) raw) {
            String name = o.get("optionName") != null ? o.get("optionName").toString().trim() : "";
            Object a = o.get("amount");
            long amount = (a != null && !a.toString().isBlank()) ? Long.parseLong(a.toString()) : 0L;
            if (name.isEmpty()) throw new IllegalArgumentException("옵션을 선택해주세요.");
            if (amount <= 0) throw new IllegalArgumentException("옵션금액을 입력해주세요.");
            String pay = o.get("paymentType") != null ? o.get("paymentType").toString() : "";
            list.add(new GuideIncomeOption(null, name, amount, resolvePay(payType, pay)));
        }
        return list;
    }

    private String resolvePay(String incomePay, String optionPay) {
        if (CHOOSE_PAY.contains(incomePay)) {
            if (!"현금".equals(optionPay) && !"카드".equals(optionPay))
                throw new IllegalArgumentException("옵션마다 현금 / 카드를 골라주세요.");
            return optionPay;
        }
        // 현금 · 카드 · 그외-현금 · 그외-카드: 수입 결제유형을 따름
        return incomePay.endsWith("카드") ? "카드" : "현금";
    }

    private static void set(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
