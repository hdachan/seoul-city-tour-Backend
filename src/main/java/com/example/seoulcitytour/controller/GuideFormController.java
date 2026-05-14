package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.GuideDailyFee;
import com.example.seoulcitytour.entity.GuideExpense;
import com.example.seoulcitytour.entity.GuideIncome;
import com.example.seoulcitytour.entity.GuideMonthLock;
import com.example.seoulcitytour.entity.TourName;
import com.example.seoulcitytour.repository.GuideDailyFeeRepository;
import com.example.seoulcitytour.repository.GuideExpenseRepository;
import com.example.seoulcitytour.repository.GuideIncomeRepository;
import com.example.seoulcitytour.repository.GuideMonthLockRepository;
import com.example.seoulcitytour.repository.TourNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/guide-form")
@RequiredArgsConstructor
public class GuideFormController {

    private final GuideIncomeRepository    incomeRepository;
    private final GuideExpenseRepository   expenseRepository;
    private final GuideDailyFeeRepository  dailyFeeRepository;
    private final TourNameRepository       tourNameRepository;
    private final GuideMonthLockRepository lockRepository;

    private boolean isMonthLocked(String guideUsername) {
        LocalDate now = LocalDate.now();
        return lockRepository
                .findByGuideUsernameAndYearAndMonth(guideUsername, now.getYear(), now.getMonthValue())
                .map(GuideMonthLock::getLocked).orElse(false);
    }

    // ── 투어이름 ──
    @GetMapping("/tour-names")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form') " +
            "or @tabPermissionService.hasAccess(authentication, 'guide-admin')")
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

    // ── 잠금 상태 ──
    @GetMapping("/lock-status")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> getLockStatus(Authentication auth) {
        return ResponseEntity.ok(Map.of("locked", isMonthLocked(auth.getName())));
    }

    // ── 수입 조회 ──
    @GetMapping("/records")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> getRecords(Authentication auth) {
        LocalDate now = LocalDate.now();
        var list = incomeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(
                auth.getName(), now.getYear(), now.getMonthValue());
        return ResponseEntity.ok(list.stream().map(i -> Map.of(
                "id",                 i.getId(),
                "tourName",           i.getTourName(),
                "representativeName", i.getRepresentativeName() != null ? i.getRepresentativeName() : "",
                "amount",             i.getAmount() != null ? i.getAmount() : 0L,
                "headcount",          i.getHeadcount() != null ? i.getHeadcount() : 0,
                "totalAmount",        i.getTotalAmount() != null ? i.getTotalAmount() : 0L,
                "paymentType",        i.getPaymentType()
        )).toList());
    }

    // ── 수입 추가 ──
    @PostMapping("/records")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> addRecord(@RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            LocalDate date = LocalDate.now();
            long amount    = parseL(body, "amount");
            int headcount  = parseI(body, "headcount");
            String payType = (String) body.get("paymentType");

            GuideIncome income = new GuideIncome();
            setField(income, "guideUsername",      auth.getName());
            setField(income, "tourName",           (String) body.get("tourName"));
            setField(income, "representativeName", body.getOrDefault("representativeName", ""));
            setField(income, "paymentType",        payType);
            setField(income, "date",               date);
            setField(income, "year",               date.getYear());
            setField(income, "month",              date.getMonthValue());
            setField(income, "locked",             false);
            if ("카드".equals(payType) || "현금".equals(payType)) {
                setField(income, "amount",      amount);
                setField(income, "headcount",   headcount);
                setField(income, "totalAmount", amount * headcount);
            }
            incomeRepository.save(income);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    // ── 수입 수정 ──
    @PutMapping("/records/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> updateRecord(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            GuideIncome income = incomeRepository.findById(id).orElseThrow();
            if (!income.getGuideUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 수정 가능합니다."));

            long amount    = parseL(body, "amount");
            int headcount  = parseI(body, "headcount");
            String payType = (String) body.get("paymentType");

            setField(income, "tourName",           (String) body.get("tourName"));
            setField(income, "representativeName", body.getOrDefault("representativeName", ""));
            setField(income, "paymentType",        payType);
            if ("카드".equals(payType) || "현금".equals(payType)) {
                setField(income, "amount",      amount);
                setField(income, "headcount",   headcount);
                setField(income, "totalAmount", amount * headcount);
            } else {
                setField(income, "amount",      0L);
                setField(income, "headcount",   0);
                setField(income, "totalAmount", 0L);
            }
            incomeRepository.save(income);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    // ── 수입 삭제 ──
    @DeleteMapping("/records/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            GuideIncome i = incomeRepository.findById(id).orElseThrow();
            if (!i.getGuideUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 삭제 가능합니다."));
            incomeRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 지출 조회 ──
    @GetMapping("/expense")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> getExpense(Authentication auth) {
        LocalDate now = LocalDate.now();
        var list = expenseRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(
                auth.getName(), now.getYear(), now.getMonthValue());
        return ResponseEntity.ok(list.stream().map(e -> Map.of(
                "id",          e.getId(),
                "expenseType", e.getExpenseType(),
                "amount",      e.getAmount(),
                "headcount",   e.getHeadcount() != null ? e.getHeadcount() : 0,
                "totalAmount", e.getTotalAmount() != null ? e.getTotalAmount() : 0L,
                "paymentType", e.getPaymentType(),
                "date",        e.getDate().toString()
        )).toList());
    }

    // ── 지출 추가 ──
    @PostMapping("/expense")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> addExpense(@RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            LocalDate date = LocalDate.now();
            long amount    = parseL(body, "amount");
            int headcount  = parseI(body, "headcount");

            GuideExpense expense = new GuideExpense();
            setField(expense, "guideUsername", auth.getName());
            setField(expense, "expenseType",   (String) body.get("expenseType"));
            setField(expense, "amount",        amount);
            setField(expense, "headcount",     headcount);
            setField(expense, "totalAmount",   amount * headcount);
            setField(expense, "paymentType",   (String) body.get("paymentType"));
            setField(expense, "date",          date);
            setField(expense, "year",          date.getYear());
            setField(expense, "month",         date.getMonthValue());
            setField(expense, "locked",        false);
            expenseRepository.save(expense);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    // ── 지출 수정 ──
    @PutMapping("/expense/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> updateExpense(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            GuideExpense expense = expenseRepository.findById(id).orElseThrow();
            if (!expense.getGuideUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 수정 가능합니다."));

            long amount   = parseL(body, "amount");
            int headcount = parseI(body, "headcount");
            setField(expense, "expenseType",  (String) body.get("expenseType"));
            setField(expense, "amount",       amount);
            setField(expense, "headcount",    headcount);
            setField(expense, "totalAmount",  amount * headcount);
            setField(expense, "paymentType",  (String) body.get("paymentType"));
            expenseRepository.save(expense);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    // ── 지출 삭제 ──
    @DeleteMapping("/expense/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            GuideExpense e = expenseRepository.findById(id).orElseThrow();
            if (!e.getGuideUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 삭제 가능합니다."));
            expenseRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 일비 조회 ──
    @GetMapping("/daily-fee")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> getDailyFee(Authentication auth) {
        LocalDate now = LocalDate.now();
        var list = dailyFeeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(
                auth.getName(), now.getYear(), now.getMonthValue());
        return ResponseEntity.ok(list.stream().map(d -> Map.of(
                "id",     d.getId(),
                "amount", d.getAmount(),
                "date",   d.getDate().toString()
        )).toList());
    }

    // ── 일비 추가 ──
    @PostMapping("/daily-fee")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> addDailyFee(@RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            LocalDate date = LocalDate.parse((String) body.get("date"));
            GuideDailyFee fee = new GuideDailyFee();
            setField(fee, "guideUsername", auth.getName());
            setField(fee, "amount",        parseL(body, "amount"));
            setField(fee, "date",          date);
            setField(fee, "year",          date.getYear());
            setField(fee, "month",         date.getMonthValue());
            setField(fee, "locked",        false);
            dailyFeeRepository.save(fee);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    // ── 일비 수정 ──
    @PutMapping("/daily-fee/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> updateDailyFee(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body,
                                            Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            GuideDailyFee fee = dailyFeeRepository.findById(id).orElseThrow();
            if (!fee.getGuideUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 수정 가능합니다."));
            LocalDate date = LocalDate.parse((String) body.get("date"));
            setField(fee, "amount", parseL(body, "amount"));
            setField(fee, "date",   date);
            setField(fee, "year",   date.getYear());
            setField(fee, "month",  date.getMonthValue());
            dailyFeeRepository.save(fee);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    // ── 일비 삭제 ──
    @DeleteMapping("/daily-fee/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> deleteDailyFee(@PathVariable Long id, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            GuideDailyFee d = dailyFeeRepository.findById(id).orElseThrow();
            if (!d.getGuideUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 삭제 가능합니다."));
            dailyFeeRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    private long parseL(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return (v != null && !v.toString().isBlank()) ? Long.parseLong(v.toString()) : 0L;
    }
    private int parseI(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return (v != null && !v.toString().isBlank()) ? Integer.parseInt(v.toString()) : 0;
    }
    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
