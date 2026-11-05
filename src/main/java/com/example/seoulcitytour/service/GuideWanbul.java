package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.GuideIncome;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 완불 수입을 대표자 여러 명으로 한 번에 추가할 때, 사람마다 한 건씩 만듦 (가이드 정산 · 가이드 정산관리 공용) */
public final class GuideWanbul {

    private GuideWanbul() {}

    @SuppressWarnings("unchecked")
    public static List<GuideIncome> build(String guideUsername, Map<String, Object> body) throws Exception {
        String tourName = (String) body.get("tourName");
        if (tourName == null || tourName.isBlank()) throw new IllegalArgumentException("투어이름을 선택해주세요.");
        List<Map<String, Object>> reps = (List<Map<String, Object>>) body.get("reps");
        if (reps == null || reps.isEmpty()) throw new IllegalArgumentException("대표자를 입력해주세요.");

        String dateStr = (String) body.get("date");
        LocalDate date = (dateStr != null && !dateStr.isBlank())
                ? LocalDate.parse(dateStr)
                : LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        List<GuideIncome> list = new ArrayList<>();
        for (Map<String, Object> rep : reps) {
            String name = rep.get("representativeName") != null ? rep.get("representativeName").toString().trim() : "";
            int adult  = count(rep, "adult");
            int child  = count(rep, "child");
            int infant = count(rep, "infant");
            if (name.isEmpty()) throw new IllegalArgumentException("대표자 이름을 모두 입력해주세요.");
            if (adult + child + infant <= 0) throw new IllegalArgumentException("대표자마다 인원을 입력해주세요.");

            GuideIncome income = new GuideIncome();
            set(income, "guideUsername",      guideUsername);
            set(income, "tourName",           tourName);
            set(income, "representativeName", name);
            set(income, "paymentType",        "완불");
            set(income, "date",               date);
            set(income, "year",               date.getYear());
            set(income, "month",              date.getMonthValue());
            set(income, "locked",             false);
            set(income, "note",               body.getOrDefault("note", ""));
            set(income, "memo",               body.getOrDefault("memo", ""));
            set(income, "amount",             0L);
            set(income, "childAmount",        0L);
            set(income, "headcount",          adult + child + infant);
            set(income, "adult",              adult);
            set(income, "child",              child);
            set(income, "infant",             infant);
            set(income, "totalAmount",        0L);
            list.add(income);
        }
        return list;
    }

    private static int count(Map<String, Object> rep, String key) {
        Object v = rep.get(key);
        return (v != null && !v.toString().isBlank()) ? Integer.parseInt(v.toString()) : 0;
    }

    private static void set(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
