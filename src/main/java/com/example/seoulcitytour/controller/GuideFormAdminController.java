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
        return ResponseEntity.ok(list.stream().map(i -> Map.of(
                "id",                 i.getId(),
                "tourName",           i.getTourName(),
                "representativeName", i.getRepresentativeName() != null ? i.getRepresentativeName() : "",
                "amount",             i.getAmount() != null ? i.getAmount() : 0L,
                "headcount",          i.getHeadcount() != null ? i.getHeadcount() : 0,
                "totalAmount",        i.getTotalAmount() != null ? i.getTotalAmount() : 0L,
                "paymentType",        i.getPaymentType(),
                "date",               i.getDate().toString()
        )).toList());
    }

    // ── 수입 추가 ──
    @PostMapping("/income")
    public ResponseEntity<?> addIncome(@RequestBody Map<String, Object> body) {
        try {
            String    guideUsername = (String) body.get("guideUsername");
            LocalDate date          = LocalDate.now();
            long      amount        = parseL(body, "amount");
            int       headcount     = parseI(body, "headcount");
            String    payType       = (String) body.get("paymentType");

            GuideIncome income = new GuideIncome();
            setField(income, "guideUsername",      guideUsername);
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
    @PutMapping("/income/{id}")
    public ResponseEntity<?> updateIncome(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            GuideIncome income = incomeRepository.findById(id).orElseThrow();
            long   amount    = parseL(body, "amount");
            int    headcount = parseI(body, "headcount");
            String payType   = (String) body.get("paymentType");
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

    // ── 지출 조회 ──
    @GetMapping("/expense")
    public ResponseEntity<?> getExpense(@RequestParam String guideUsername,
                                        @RequestParam Integer year, @RequestParam Integer month) {
        var list = expenseRepository.findByGuideUsernameAndYearAndMonthOrderByDateAsc(guideUsername, year, month);
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
    public ResponseEntity<?> addExpense(@RequestBody Map<String, Object> body) {
        try {
            String    guideUsername = (String) body.get("guideUsername");
            LocalDate date          = LocalDate.now();
            long      amount        = parseL(body, "amount");
            int       headcount     = parseI(body, "headcount");

            GuideExpense expense = new GuideExpense();
            setField(expense, "guideUsername", guideUsername);
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
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            GuideExpense expense = expenseRepository.findById(id).orElseThrow();
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