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
    private final com.example.seoulcitytour.repository.GuideExpenseCategoryRepository expenseCategoryRepository;
    private final GuideMonthLockRepository lockRepository;

    private boolean isMonthLocked(String guideUsername) {
        LocalDate now = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        return lockRepository
                .findByGuideUsernameAndYearAndMonth(guideUsername, now.getYear(), now.getMonthValue())
                .map(GuideMonthLock::getLocked).orElse(false);
    }

    // ── 투어이름 ──
    @GetMapping("/expense-categories")
    public ResponseEntity<?> getExpenseCategories(@RequestParam(required = false) Long tourNameId) {
        var list = tourNameId != null
                ? expenseCategoryRepository.findByTourNameIdAndActiveTrueOrderByNameAsc(tourNameId)
                : expenseCategoryRepository.findByActiveTrueOrderByNameAsc();
        return ResponseEntity.ok(list.stream()
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "tourNameId", c.getTourNameId() != null ? c.getTourNameId() : 0L))
                .toList());
    }

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
    public ResponseEntity<?> getRecords(Authentication auth,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        int y = year != null ? year : now.getYear();
        int mo = month != null ? month : now.getMonthValue();
        System.out.println("=== getRecords: username=" + auth.getName() + " year=" + y + " month=" + mo + " count=" + incomeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(auth.getName(), y, mo).size());
        var list = incomeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(
                auth.getName(), y, mo);
        return ResponseEntity.ok(list.stream().map(i -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id",                 i.getId());
            m.put("tourName",           i.getTourName());
            m.put("representativeName", i.getRepresentativeName() != null ? i.getRepresentativeName() : "");
            m.put("amount",             i.getAmount() != null ? i.getAmount() : 0L);
            m.put("headcount",          i.getHeadcount() != null ? i.getHeadcount() : 0);
            m.put("adult",              i.getAdult() != null ? i.getAdult() : 0);
            m.put("child",              i.getChild() != null ? i.getChild() : 0);
            m.put("childAmount",        i.getChildAmount() != null ? i.getChildAmount() : 0L);
            m.put("infant",             i.getInfant() != null ? i.getInfant() : 0);
            m.put("totalAmount",        i.getTotalAmount() != null ? i.getTotalAmount() : 0L);
            m.put("note",               i.getNote() != null ? i.getNote() : "");
            m.put("memo",               i.getMemo() != null ? i.getMemo() : "");
            m.put("paymentType",        i.getPaymentType());
            m.put("date",               i.getDate() != null ? i.getDate().toString() : "");
            return m;
        }).toList());
    }

    // ── 수입 추가 ──
    @PostMapping("/records")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> addRecord(@RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            LocalDate date = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
            long amount    = parseL(body, "amount");
            int headcount  = parseI(body, "headcount");
            String payType = (String) body.get("paymentType");

            String dateStr = (String) body.get("date");
            LocalDate incomeDate = (dateStr != null && !dateStr.isBlank())
                    ? LocalDate.parse(dateStr)
                    : LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

            GuideIncome income = new GuideIncome();
            setField(income, "guideUsername",      auth.getName());
            setField(income, "tourName",           (String) body.get("tourName"));
            setField(income, "representativeName", body.getOrDefault("representativeName", ""));
            setField(income, "paymentType",        payType);
            setField(income, "date",               incomeDate);
            setField(income, "year",               incomeDate.getYear());
            setField(income, "month",              incomeDate.getMonthValue());
            setField(income, "locked",             false);
            int  adult       = parseI(body, "adult");
            int  child       = parseI(body, "child");
            long childAmount = parseL(body, "childAmount");
            int  infant      = parseI(body, "infant");
            if (adult > 0 || child > 0 || infant > 0) headcount = adult + child + infant;

            if ("완불".equals(payType) || "그외".equals(payType)) {
                setField(income, "amount",      0L);
                setField(income, "headcount",   headcount);
                setField(income, "adult",       adult);
                setField(income, "child",       child);
                setField(income, "childAmount", 0L);
                setField(income, "infant",      infant);
                setField(income, "totalAmount", 0L);
            } else {
                long totalAmt = amount * adult + childAmount * child;
                setField(income, "amount",      amount);
                setField(income, "headcount",   headcount);
                setField(income, "adult",       adult);
                setField(income, "child",       child);
                setField(income, "childAmount", childAmount);
                setField(income, "infant",      infant);
                setField(income, "totalAmount", totalAmt);
            }
            setField(income, "note", body.getOrDefault("note", ""));
            setField(income, "memo", body.getOrDefault("memo", ""));
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

            long amount      = parseL(body, "amount");
            long childAmount = parseL(body, "childAmount");
            int  headcount   = parseI(body, "headcount");
            int  adult       = parseI(body, "adult");
            int  child       = parseI(body, "child");
            int  infant      = parseI(body, "infant");
            String payType   = (String) body.get("paymentType");
            if (adult > 0 || child > 0 || infant > 0) headcount = adult + child + infant;

            String dateStr2 = (String) body.get("date");
            if (dateStr2 != null && !dateStr2.isBlank()) {
                LocalDate newDate = LocalDate.parse(dateStr2);
                setField(income, "date",  newDate);
                setField(income, "year",  newDate.getYear());
                setField(income, "month", newDate.getMonthValue());
            }
            setField(income, "tourName",           (String) body.get("tourName"));
            setField(income, "representativeName", body.getOrDefault("representativeName", ""));
            setField(income, "paymentType",        payType);
            if ("완불".equals(payType) || "그외".equals(payType)) {
                setField(income, "amount",      0L);
                setField(income, "headcount",   headcount);
                setField(income, "adult",       adult);
                setField(income, "child",       child);
                setField(income, "childAmount", 0L);
                setField(income, "infant",      infant);
                setField(income, "totalAmount", 0L);
            } else {
                long totalAmt = amount * adult + childAmount * child;
                setField(income, "amount",      amount);
                setField(income, "headcount",   headcount);
                setField(income, "adult",       adult);
                setField(income, "child",       child);
                setField(income, "childAmount", childAmount);
                setField(income, "infant",      infant);
                setField(income, "totalAmount", totalAmt);
            }
            setField(income, "note", body.getOrDefault("note", ""));
            setField(income, "memo", body.getOrDefault("memo", ""));
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
    public ResponseEntity<?> getExpense(Authentication auth,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        int y = year != null ? year : now.getYear();
        int mo = month != null ? month : now.getMonthValue();
        var list = expenseRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(
                auth.getName(), y, mo);
        return ResponseEntity.ok(list.stream().map(e -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id",          e.getId());
            m.put("tourName",    e.getTourName() != null ? e.getTourName() : "");
            m.put("expenseType", e.getExpenseType());
            m.put("amount",      e.getAmount());
            m.put("headcount",   e.getHeadcount() != null ? e.getHeadcount() : 0);
            m.put("totalAmount", e.getTotalAmount() != null ? e.getTotalAmount() : 0L);
            m.put("paymentType", e.getPaymentType());
            m.put("memo",        e.getMemo() != null ? e.getMemo() : "");
            m.put("date",        e.getDate().toString());
            return m;
        }).toList());
    }

    // ── 지출 추가 ──
    @PostMapping("/expense")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-form')")
    public ResponseEntity<?> addExpense(@RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 관리자에 의해 잠겨있습니다."));
        try {
            LocalDate date = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
            long amount    = parseL(body, "amount");
            int headcount  = parseI(body, "headcount");

            GuideExpense expense = new GuideExpense();
            setField(expense, "guideUsername", auth.getName());
            setField(expense, "tourName",      body.getOrDefault("tourName", ""));
            setField(expense, "expenseType",   (String) body.get("expenseType"));
            setField(expense, "amount",        amount);
            setField(expense, "headcount",     headcount);
            setField(expense, "totalAmount",   amount * headcount);
            setField(expense, "paymentType",   (String) body.get("paymentType"));
            setField(expense, "memo",          body.getOrDefault("memo", ""));
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
            setField(expense, "tourName",     body.getOrDefault("tourName", ""));
            setField(expense, "expenseType",  (String) body.get("expenseType"));
            setField(expense, "amount",       amount);
            setField(expense, "headcount",    headcount);
            setField(expense, "totalAmount",  amount * headcount);
            setField(expense, "paymentType",  (String) body.get("paymentType"));
            setField(expense, "memo",         body.getOrDefault("memo", ""));
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
    public ResponseEntity<?> getDailyFee(Authentication auth,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        int y = year != null ? year : now.getYear();
        int mo = month != null ? month : now.getMonthValue();
        var list = dailyFeeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(
                auth.getName(), y, mo);
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
    // ── 통계 ──
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestParam String username,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        var incomes = incomeRepository.findByGuideUsernameAndYearAndMonth(username,
                year != null ? year : LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).getYear(),
                month != null ? month : LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).getMonthValue());

        var expenses = expenseRepository.findByGuideUsernameAndYearAndMonth(username,
                year != null ? year : LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).getYear(),
                month != null ? month : LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).getMonthValue());

        // 투어별 통계
        java.util.Map<String, java.util.Map<String, Object>> tourStats = new java.util.LinkedHashMap<>();
        for (var i : incomes) {
            tourStats.computeIfAbsent(i.getTourName(), k -> {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("tourName", k); m.put("count", 0); m.put("totalAdult", 0);
                m.put("totalChild", 0); m.put("totalInfant", 0); m.put("totalAmount", 0L);
                return m;
            });
            var t = tourStats.get(i.getTourName());
            t.put("count", (int) t.get("count") + 1);
            t.put("totalAdult", (int) t.get("totalAdult") + (i.getAdult() != null ? i.getAdult() : 0));
            t.put("totalChild", (int) t.get("totalChild") + (i.getChild() != null ? i.getChild() : 0));
            t.put("totalInfant", (int) t.get("totalInfant") + (i.getInfant() != null ? i.getInfant() : 0));
            t.put("totalAmount", (long) t.get("totalAmount") + (i.getTotalAmount() != null ? i.getTotalAmount() : 0L));
        }

        // 결제수단별 합계
        long cashTotal = incomes.stream().filter(i -> "현금".equals(i.getPaymentType()) || "그외-현금".equals(i.getPaymentType()))
                .mapToLong(i -> i.getTotalAmount() != null ? i.getTotalAmount() : 0L).sum();
        long cardTotal = incomes.stream().filter(i -> "카드".equals(i.getPaymentType()) || "그외-카드".equals(i.getPaymentType()))
                .mapToLong(i -> i.getTotalAmount() != null ? i.getTotalAmount() : 0L).sum();
        long wanbul = incomes.stream().filter(i -> "완불".equals(i.getPaymentType()))
                .mapToLong(i -> i.getTotalAmount() != null ? i.getTotalAmount() : 0L).sum();
        long totalIncome = incomes.stream().mapToLong(i -> i.getTotalAmount() != null ? i.getTotalAmount() : 0L).sum();
        long totalExpense = expenses.stream().mapToLong(e -> e.getTotalAmount() != null ? e.getTotalAmount() : 0L).sum();
        int totalAdult = incomes.stream().mapToInt(i -> i.getAdult() != null ? i.getAdult() : 0).sum();
        int totalChild = incomes.stream().mapToInt(i -> i.getChild() != null ? i.getChild() : 0).sum();
        int totalInfant = incomes.stream().mapToInt(i -> i.getInfant() != null ? i.getInfant() : 0).sum();

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("totalTours", incomes.size());
        result.put("totalAdult", totalAdult);
        result.put("totalChild", totalChild);
        result.put("totalInfant", totalInfant);
        result.put("totalIncome", totalIncome);
        result.put("cashTotal", cashTotal);
        result.put("cardTotal", cardTotal);
        result.put("wanbul", wanbul);
        result.put("totalExpense", totalExpense);
        result.put("netIncome", totalIncome - totalExpense);
        result.put("tourStats", tourStats.values());
        return ResponseEntity.ok(result);
    }
}