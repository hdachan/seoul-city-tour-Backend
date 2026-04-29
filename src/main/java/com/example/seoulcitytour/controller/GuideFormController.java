package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.GuideDailyFee;
import com.example.seoulcitytour.entity.GuideExpense;
import com.example.seoulcitytour.entity.GuideIncome;
import com.example.seoulcitytour.entity.TourName;
import com.example.seoulcitytour.repository.GuideDailyFeeRepository;
import com.example.seoulcitytour.repository.GuideExpenseRepository;
import com.example.seoulcitytour.repository.GuideIncomeRepository;
import com.example.seoulcitytour.repository.TourNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/guide-form")
@RequiredArgsConstructor
public class GuideFormController {

    private final GuideIncomeRepository   incomeRepository;
    private final GuideExpenseRepository  expenseRepository;
    private final GuideDailyFeeRepository dailyFeeRepository;
    private final TourNameRepository      tourNameRepository;

    // ── 투어이름 ──
    @GetMapping("/tour-names")
    @PreAuthorize("hasAnyRole('GUIDE', 'ADMIN', 'DEV')")
    public ResponseEntity<?> getTourNames() {
        return ResponseEntity.ok(tourNameRepository.findByActiveTrueOrderByNameAsc()
                .stream().map(t -> Map.of("id", t.getId(), "name", t.getName())).toList());
    }

    @PostMapping("/tour-names")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addTourName(@RequestBody Map<String, String> body) {
        try {
            TourName t = new TourName();
            setField(t, "name", body.get("name").trim());
            setField(t, "active", true);
            tourNameRepository.save(t);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/tour-names/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTourName(@PathVariable Long id) {
        try {
            TourName t = tourNameRepository.findById(id).orElseThrow();
            setField(t, "active", false);
            tourNameRepository.save(t);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 수입 기록 ──
    @GetMapping("/records")
    @PreAuthorize("hasRole('GUIDE')")
    public ResponseEntity<?> getRecords(@RequestParam Integer year,
                                        @RequestParam Integer month,
                                        Authentication auth) {
        var list = incomeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(auth.getName(), year, month);
        return ResponseEntity.ok(list.stream().map(i -> Map.of(
                "id",                 i.getId(),
                "tourName",           i.getTourName(),
                "representativeName", i.getRepresentativeName() != null ? i.getRepresentativeName() : "",
                "amount",             i.getAmount() != null ? i.getAmount() : 0L,
                "headcount",          i.getHeadcount() != null ? i.getHeadcount() : 0,
                "totalAmount",        i.getTotalAmount() != null ? i.getTotalAmount() : 0L,
                "paymentType",        i.getPaymentType(),
                "locked",             i.getLocked()
        )).toList());
    }

    @PostMapping("/records")
    @PreAuthorize("hasRole('GUIDE')")
    public ResponseEntity<?> addRecord(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            LocalDate date     = LocalDate.now();
            boolean autoLocked = YearMonth.of(date.getYear(), date.getMonthValue()).isBefore(YearMonth.now());

            long amount   = body.get("amount") != null && !body.get("amount").toString().isBlank()
                    ? Long.parseLong(body.get("amount").toString()) : 0L;
            int headcount = body.get("headcount") != null && !body.get("headcount").toString().isBlank()
                    ? Integer.parseInt(body.get("headcount").toString()) : 0;
            long total = amount * headcount;

            GuideIncome income = new GuideIncome();
            setField(income, "guideUsername",      auth.getName());
            setField(income, "tourName",           (String) body.get("tourName"));
            setField(income, "representativeName", body.getOrDefault("representativeName", ""));
            setField(income, "paymentType",        (String) body.get("paymentType"));
            setField(income, "date",               date);
            setField(income, "year",               date.getYear());
            setField(income, "month",              date.getMonthValue());
            setField(income, "locked",             autoLocked);

            String payType = (String) body.get("paymentType");
            if ("카드".equals(payType) || "현금".equals(payType)) {
                setField(income, "amount",      amount);
                setField(income, "headcount",   headcount);
                setField(income, "totalAmount", total);
            }
            incomeRepository.save(income);
            return ResponseEntity.ok(Map.of("message", "기록이 추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasRole('GUIDE')")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id, Authentication auth) {
        try {
            GuideIncome i = incomeRepository.findById(id).orElseThrow();
            if (!i.getGuideUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 삭제 가능합니다."));
            if (i.getLocked())
                return ResponseEntity.badRequest().body(Map.of("error", "잠긴 항목은 삭제할 수 없습니다."));
            incomeRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 지출 ──
    @GetMapping("/expense")
    @PreAuthorize("hasRole('GUIDE')")
    public ResponseEntity<?> getExpense(@RequestParam Integer year,
                                        @RequestParam Integer month,
                                        Authentication auth) {
        var list = expenseRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(auth.getName(), year, month);
        return ResponseEntity.ok(list.stream().map(e -> Map.of(
                "id",          e.getId(),
                "expenseType", e.getExpenseType(),
                "amount",      e.getAmount(),
                "headcount",   e.getHeadcount() != null ? e.getHeadcount() : 0,
                "totalAmount", e.getTotalAmount() != null ? e.getTotalAmount() : 0L,
                "paymentType", e.getPaymentType(),
                "date",        e.getDate().toString(),
                "locked",      e.getLocked()
        )).toList());
    }

    @PostMapping("/expense")
    @PreAuthorize("hasRole('GUIDE')")
    public ResponseEntity<?> addExpense(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            // 날짜 자동입력
            LocalDate date     = LocalDate.now();
            boolean autoLocked = YearMonth.of(date.getYear(), date.getMonthValue()).isBefore(YearMonth.now());

            long amount   = Long.parseLong(body.get("amount").toString());
            int headcount = body.get("headcount") != null && !body.get("headcount").toString().isBlank()
                    ? Integer.parseInt(body.get("headcount").toString()) : 1;
            long total = amount * headcount;

            GuideExpense expense = new GuideExpense();
            setField(expense, "guideUsername", auth.getName());
            setField(expense, "expenseType",   (String) body.get("expenseType"));
            setField(expense, "amount",        amount);
            setField(expense, "headcount",     headcount);
            setField(expense, "totalAmount",   total);
            setField(expense, "paymentType",   (String) body.get("paymentType"));
            setField(expense, "date",          date);
            setField(expense, "year",          date.getYear());
            setField(expense, "month",         date.getMonthValue());
            setField(expense, "locked",        autoLocked);
            expenseRepository.save(expense);
            return ResponseEntity.ok(Map.of("message", "지출이 추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    @DeleteMapping("/expense/{id}")
    @PreAuthorize("hasRole('GUIDE')")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id, Authentication auth) {
        try {
            GuideExpense e = expenseRepository.findById(id).orElseThrow();
            if (!e.getGuideUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 삭제 가능합니다."));
            if (e.getLocked())
                return ResponseEntity.badRequest().body(Map.of("error", "잠긴 항목은 삭제할 수 없습니다."));
            expenseRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 일비 ──
    @GetMapping("/daily-fee")
    @PreAuthorize("hasRole('GUIDE')")
    public ResponseEntity<?> getDailyFee(@RequestParam Integer year,
                                         @RequestParam Integer month,
                                         Authentication auth) {
        var list = dailyFeeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(auth.getName(), year, month);
        return ResponseEntity.ok(list.stream().map(d -> Map.of(
                "id",     d.getId(),
                "amount", d.getAmount(),
                "date",   d.getDate().toString(),
                "locked", d.getLocked()
        )).toList());
    }

    @PostMapping("/daily-fee")
    @PreAuthorize("hasRole('GUIDE')")
    public ResponseEntity<?> addDailyFee(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            LocalDate date     = LocalDate.parse((String) body.get("date"));
            boolean autoLocked = YearMonth.of(date.getYear(), date.getMonthValue()).isBefore(YearMonth.now());

            GuideDailyFee fee = new GuideDailyFee();
            setField(fee, "guideUsername", auth.getName());
            setField(fee, "amount",        Long.parseLong(body.get("amount").toString()));
            setField(fee, "date",          date);
            setField(fee, "year",          date.getYear());
            setField(fee, "month",         date.getMonthValue());
            setField(fee, "locked",        autoLocked);
            dailyFeeRepository.save(fee);
            return ResponseEntity.ok(Map.of("message", "일비가 추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    @DeleteMapping("/daily-fee/{id}")
    @PreAuthorize("hasRole('GUIDE')")
    public ResponseEntity<?> deleteDailyFee(@PathVariable Long id, Authentication auth) {
        try {
            GuideDailyFee d = dailyFeeRepository.findById(id).orElseThrow();
            if (!d.getGuideUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 삭제 가능합니다."));
            if (d.getLocked())
                return ResponseEntity.badRequest().body(Map.of("error", "잠긴 항목은 삭제할 수 없습니다."));
            dailyFeeRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
