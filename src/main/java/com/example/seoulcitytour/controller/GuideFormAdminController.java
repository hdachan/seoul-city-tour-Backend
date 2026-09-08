package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.GuideDailyFee;
import com.example.seoulcitytour.entity.GuideExpense;
import com.example.seoulcitytour.entity.GuideIncome;
import com.example.seoulcitytour.entity.GuideMonthLock;
import com.example.seoulcitytour.repository.GuideDailyFeeRepository;
import com.example.seoulcitytour.repository.GuideExpenseRepository;
import com.example.seoulcitytour.repository.GuideIncomeRepository;
import com.example.seoulcitytour.repository.GuideMonthLockRepository;
import com.example.seoulcitytour.repository.TourNameRepository;
import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/guide-admin")
@PreAuthorize("@tabPermissionService.hasAccess(authentication, 'guide-admin')")
@RequiredArgsConstructor
public class GuideFormAdminController {

    private final UserRepository           userRepository;
    private final GuideIncomeRepository    incomeRepository;
    private final GuideExpenseRepository   expenseRepository;
    private final GuideDailyFeeRepository  dailyFeeRepository;
    private final GuideMonthLockRepository lockRepository;
    private final TourNameRepository       tourNameRepository;
    private final com.example.seoulcitytour.repository.GuideExpenseCategoryRepository expenseCategoryRepository;

    // ── 가이드 목록 (active=true만) ──
    @GetMapping("/guides")
    public ResponseEntity<?> getGuides() {
        return ResponseEntity.ok(userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> "ROLE_GUIDE".equals(u.getRole()))
                .map(u -> Map.of(
                        "username", u.getUsername(),
                        "name",     u.getName() != null ? u.getName() : u.getUsername()
                )).toList());
    }

    // ── 투어이름 목록 (guide-admin 에서도 접근 가능) ──
    @GetMapping("/tour-names")
    public ResponseEntity<?> getTourNames() {
        return ResponseEntity.ok(tourNameRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(t -> Map.of("id", t.getId(), "name", t.getName()))
                .toList());
    }

    @GetMapping("/tour-names/all")
    public ResponseEntity<?> getAllTourNames() {
        return ResponseEntity.ok(tourNameRepository.findAll().stream()
                .filter(t -> t.getActive())
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .map(t -> Map.of("id", t.getId(), "name", t.getName()))
                .toList());
    }

    @PostMapping("/tour-names")
    public ResponseEntity<?> addTourName(@RequestBody Map<String, Object> body) {
        try {
            String name = ((String) body.get("name")).trim();
            if (name.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "이름을 입력해주세요."));
            if (tourNameRepository.existsByName(name))
                return ResponseEntity.badRequest().body(Map.of("error", "이미 존재하는 투어입니다."));
            com.example.seoulcitytour.entity.TourName t = new com.example.seoulcitytour.entity.TourName();
            setField(t, "name",   name);
            setField(t, "active", true);
            tourNameRepository.save(t);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/tour-names/{id}")
    public ResponseEntity<?> deleteTourName(@PathVariable Long id) {
        try {
            tourNameRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 월별 입력 현황 요약 (카드뷰 - active=true만) ──
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam Integer year, @RequestParam Integer month) {
        var guides = userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> "ROLE_GUIDE".equals(u.getRole()))
                .toList();

        var result = guides.stream().map(g -> {
            int incomeCount   = incomeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(g.getUsername(), year, month).size();
            int expenseCount  = expenseRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(g.getUsername(), year, month).size();
            int dailyFeeCount = dailyFeeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(g.getUsername(), year, month).size();
            boolean locked    = lockRepository.findByGuideUsernameAndYearAndMonth(g.getUsername(), year, month)
                    .map(GuideMonthLock::getLocked).orElse(false);
            return Map.of(
                    "username",      g.getUsername(),
                    "name",          g.getName() != null ? g.getName() : g.getUsername(),
                    "incomeCount",   incomeCount,
                    "expenseCount",  expenseCount,
                    "dailyFeeCount", dailyFeeCount,
                    "hasData",       (incomeCount + expenseCount + dailyFeeCount) > 0,
                    "locked",        locked
            );
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ── 잠금 상태 ──
    @GetMapping("/lock-status")
    public ResponseEntity<?> getLockStatus(@RequestParam String guideUsername,
                                           @RequestParam Integer year,
                                           @RequestParam Integer month) {
        boolean locked = lockRepository
                .findByGuideUsernameAndYearAndMonth(guideUsername, year, month)
                .map(GuideMonthLock::getLocked).orElse(false);
        return ResponseEntity.ok(Map.of("locked", locked));
    }

    // ── 월 잠금 토글 ──
    @PostMapping("/lock")
    public ResponseEntity<?> toggleLock(@RequestBody Map<String, Object> body) {
        try {
            String  guideUsername = (String)  body.get("guideUsername");
            Integer year          = (Integer) body.get("year");
            Integer month         = (Integer) body.get("month");
            Boolean locked        = (Boolean) body.get("locked");

            GuideMonthLock lock = lockRepository
                    .findByGuideUsernameAndYearAndMonth(guideUsername, year, month)
                    .orElse(new GuideMonthLock());
            setField(lock, "guideUsername", guideUsername);
            setField(lock, "year",          year);
            setField(lock, "month",         month);
            setField(lock, "locked",        locked);
            lockRepository.save(lock);
            return ResponseEntity.ok(Map.of("locked", locked));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 수입 조회 ──
    @GetMapping("/income")
    public ResponseEntity<?> getIncome(@RequestParam String guideUsername,
                                       @RequestParam Integer year, @RequestParam Integer month) {
        var list = incomeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(guideUsername, year, month);
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
            m.put("date",               i.getDate().toString());
            return m;
        }).toList());
    }

    // ── 수입 추가 ──
    @PostMapping("/income")
    public ResponseEntity<?> addIncome(@RequestBody Map<String, Object> body) {
        try {
            String    guideUsername = (String) body.get("guideUsername");
            long      amount        = parseL(body, "amount");
            long      childAmount   = parseL(body, "childAmount");
            int       headcount     = parseI(body, "headcount");
            int       adult         = parseI(body, "adult");
            int       child         = parseI(body, "child");
            int       infant        = parseI(body, "infant");
            if (adult > 0 || child > 0 || infant > 0) headcount = adult + child + infant;
            String    payType       = (String) body.get("paymentType");

            String dateStr = (String) body.get("date");
            LocalDate date = (dateStr != null && !dateStr.isBlank())
                    ? LocalDate.parse(dateStr)
                    : LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

            GuideIncome income = new GuideIncome();
            setField(income, "guideUsername",      guideUsername);
            setField(income, "tourName",           (String) body.get("tourName"));
            setField(income, "representativeName", body.getOrDefault("representativeName", ""));
            setField(income, "paymentType",        payType);
            setField(income, "date",               date);
            setField(income, "year",               date.getYear());
            setField(income, "month",              date.getMonthValue());
            setField(income, "locked",             false);
            setField(income, "note",               body.getOrDefault("note", ""));
            setField(income, "memo",               body.getOrDefault("memo", ""));
            if ("완불".equals(payType) || "그외".equals(payType) || payType.startsWith("그외-")) {
                setField(income, "amount",      0L);
                setField(income, "childAmount", 0L);
                setField(income, "headcount",   headcount);
                setField(income, "adult",       adult);
                setField(income, "child",       child);
                setField(income, "infant",      infant);
                setField(income, "totalAmount", 0L);
            } else {
                long totalAmt = amount * adult + childAmount * child;
                setField(income, "amount",      amount);
                setField(income, "childAmount", childAmount);
                setField(income, "headcount",   headcount);
                setField(income, "adult",       adult);
                setField(income, "child",       child);
                setField(income, "infant",      infant);
                setField(income, "totalAmount", totalAmt);
            }
            incomeRepository.save(income);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다.", "id", income.getId()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    // ── 수입 수정 ──
    @PutMapping("/income/{id}")
    public ResponseEntity<?> updateIncome(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            GuideIncome income = incomeRepository.findById(id).orElseThrow();
            long   amount      = parseL(body, "amount");
            long   childAmount = parseL(body, "childAmount");
            int    headcount   = parseI(body, "headcount");
            int    adult       = parseI(body, "adult");
            int    child       = parseI(body, "child");
            int    infant      = parseI(body, "infant");
            if (adult > 0 || child > 0 || infant > 0) headcount = adult + child + infant;
            String payType     = (String) body.get("paymentType");
            String dateStr2    = (String) body.get("date");
            if (dateStr2 != null && !dateStr2.isBlank()) {
                LocalDate newDate = LocalDate.parse(dateStr2);
                setField(income, "date",  newDate);
                setField(income, "year",  newDate.getYear());
                setField(income, "month", newDate.getMonthValue());
            }
            setField(income, "tourName",           (String) body.get("tourName"));
            setField(income, "representativeName", body.getOrDefault("representativeName", ""));
            setField(income, "paymentType",        payType);
            if ("카드".equals(payType) || "현금".equals(payType)) {
                setField(income, "amount",      amount);
                setField(income, "headcount",   headcount);
                setField(income, "totalAmount", amount * headcount);
            } else {
                setField(income, "amount", 0L); setField(income, "headcount", 0); setField(income, "totalAmount", 0L);
            }
            incomeRepository.save(income);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    // ── 수입 삭제 ──
    @DeleteMapping("/income/{id}")
    public ResponseEntity<?> deleteIncome(@PathVariable Long id) {
        incomeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 지출 카테고리 조회 ──
    @GetMapping("/expense-categories")
    public ResponseEntity<?> getExpenseCategories(@RequestParam(required = false) Long tourNameId) {
        var list = tourNameId != null
                ? expenseCategoryRepository.findByTourNameIdAndActiveTrueOrderByNameAsc(tourNameId)
                : expenseCategoryRepository.findByActiveTrueOrderByNameAsc();
        return ResponseEntity.ok(list.stream()
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "tourNameId", c.getTourNameId() != null ? c.getTourNameId() : 0L))
                .toList());
    }

    @PostMapping("/expense-categories")
    public ResponseEntity<?> addExpenseCategory(@RequestBody Map<String, Object> body) {
        try {
            String name = ((String) body.get("name")).trim();
            Long tourNameId = body.get("tourNameId") != null ? Long.parseLong(body.get("tourNameId").toString()) : null;
            if (name.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "이름을 입력해주세요."));
            if (tourNameId != null && expenseCategoryRepository.existsByNameAndTourNameId(name, tourNameId))
                return ResponseEntity.badRequest().body(Map.of("error", "이미 존재하는 카테고리입니다."));
            var cat = new com.example.seoulcitytour.entity.GuideExpenseCategory();
            setField(cat, "name", name);
            setField(cat, "tourNameId", tourNameId);
            setField(cat, "active", true);
            expenseCategoryRepository.save(cat);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/expense-categories/{id}")
    public ResponseEntity<?> deleteExpenseCategory(@PathVariable Long id) {
        try {
            expenseCategoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 지출 조회 ──
    @GetMapping("/expense")
    public ResponseEntity<?> getExpense(@RequestParam String guideUsername,
                                        @RequestParam Integer year, @RequestParam Integer month) {
        var list = expenseRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(guideUsername, year, month);
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
    public ResponseEntity<?> addExpense(@RequestBody Map<String, Object> body) {
        try {
            String    guideUsername = (String) body.get("guideUsername");
            long      amount        = parseL(body, "amount");
            int       headcount     = parseI(body, "headcount");

            String dateStr = (String) body.get("date");
            LocalDate date2 = (dateStr != null && !dateStr.isBlank())
                    ? LocalDate.parse(dateStr)
                    : LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
            GuideExpense expense = new GuideExpense();
            setField(expense, "guideUsername", guideUsername);
            setField(expense, "tourName",      body.getOrDefault("tourName", ""));
            setField(expense, "expenseType",   (String) body.get("expenseType"));
            setField(expense, "amount",        amount);
            setField(expense, "headcount",     headcount);
            setField(expense, "totalAmount",   amount * headcount);
            setField(expense, "paymentType",   (String) body.get("paymentType"));
            setField(expense, "memo",          body.getOrDefault("memo", ""));
            setField(expense, "date",          date2);
            setField(expense, "year",          date2.getYear());
            setField(expense, "month",         date2.getMonthValue());
            setField(expense, "locked",        false);
            expenseRepository.save(expense);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    // ── 지출 수정 ──
    @PutMapping("/expense/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            GuideExpense expense = expenseRepository.findById(id).orElseThrow();
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
    public ResponseEntity<?> deleteExpense(@PathVariable Long id) {
        expenseRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 일비 조회 ──
    @GetMapping("/daily-fee")
    public ResponseEntity<?> getDailyFee(@RequestParam String guideUsername,
                                         @RequestParam Integer year, @RequestParam Integer month) {
        var list = dailyFeeRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(guideUsername, year, month);
        return ResponseEntity.ok(list.stream().map(d -> Map.of(
                "id",     d.getId(),
                "amount", d.getAmount(),
                "date",   d.getDate().toString()
        )).toList());
    }

    // ── 일비 추가 ──
    @PostMapping("/daily-fee")
    public ResponseEntity<?> addDailyFee(@RequestBody Map<String, Object> body) {
        try {
            String    guideUsername = (String) body.get("guideUsername");
            LocalDate date          = LocalDate.parse((String) body.get("date"));

            GuideDailyFee fee = new GuideDailyFee();
            setField(fee, "guideUsername", guideUsername);
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
    public ResponseEntity<?> updateDailyFee(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            GuideDailyFee fee  = dailyFeeRepository.findById(id).orElseThrow();
            LocalDate     date = LocalDate.parse((String) body.get("date"));
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
    public ResponseEntity<?> deleteDailyFee(@PathVariable Long id) {
        dailyFeeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
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