package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.*;
import com.example.seoulcitytour.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-admin")
@PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales-admin')")
@RequiredArgsConstructor
public class SalesAdminController {

    private final UserRepository           userRepository;
    private final SalesReceiptRepository   receiptRepository;
    private final SalesDrivingRepository   drivingRepository;
    private final SalesMonthLockRepository lockRepository;
    private final SalesCategoryRepository  categoryRepository;
    private final SalesCashRepository cashRepository;   // ← 이거 추가

    private long calcSupplyAmount(long total) { return Math.round(total / 1.1); }
    private long calcVat(long total)          { return total - calcSupplyAmount(total); }

    // ── 영업 계정 목록 ──
    @GetMapping("/sales-users")
    public ResponseEntity<?> getSalesUsers() {
        return ResponseEntity.ok(userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> "ROLE_SALES".equals(u.getRole()))
                .map(u -> Map.of("username", u.getUsername(), "name", u.getName() != null ? u.getName() : u.getUsername()))
                .toList());
    }

    // ── 카드뷰 요약 ──
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam Integer year, @RequestParam Integer month) {
        var users = userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> "ROLE_SALES".equals(u.getRole())).toList();
        return ResponseEntity.ok(users.stream().map(u -> {
            int receiptCount = receiptRepository.findBySalesUsernameAndYearAndMonthOrderByDateAsc(u.getUsername(), year, month).size();
            int drivingCount = drivingRepository.findBySalesUsernameAndYearAndMonthOrderByIdAsc(u.getUsername(), year, month).size();
            boolean locked   = lockRepository.findBySalesUsernameAndYearAndMonth(u.getUsername(), year, month)
                    .map(SalesMonthLock::getLocked).orElse(false);
            return Map.of("username", u.getUsername(), "name", u.getName() != null ? u.getName() : u.getUsername(),
                    "receiptCount", receiptCount, "drivingCount", drivingCount,
                    "hasData", (receiptCount + drivingCount) > 0, "locked", locked);
        }).toList());
    }

    // ── 잠금 ──
    @GetMapping("/lock-status")
    public ResponseEntity<?> getLockStatus(@RequestParam String salesUsername, @RequestParam Integer year, @RequestParam Integer month) {
        boolean locked = lockRepository.findBySalesUsernameAndYearAndMonth(salesUsername, year, month)
                .map(SalesMonthLock::getLocked).orElse(false);
        return ResponseEntity.ok(Map.of("locked", locked));
    }

    @PostMapping("/lock")
    public ResponseEntity<?> toggleLock(@RequestBody Map<String, Object> body) {
        try {
            String  salesUsername = (String)  body.get("salesUsername");
            Integer year          = (Integer) body.get("year");
            Integer month         = (Integer) body.get("month");
            Boolean locked        = (Boolean) body.get("locked");
            SalesMonthLock lock = lockRepository.findBySalesUsernameAndYearAndMonth(salesUsername, year, month)
                    .orElse(new SalesMonthLock());
            setField(lock, "salesUsername", salesUsername);
            setField(lock, "year", year); setField(lock, "month", month); setField(lock, "locked", locked);
            lockRepository.save(lock);
            return ResponseEntity.ok(Map.of("locked", locked));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 카테고리 관리 (unit 포함) ──
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(categoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "unit", c.getUnit(), "active", c.getActive()))
                .toList());
    }

    @PostMapping("/categories")
    public ResponseEntity<?> addCategory(@RequestBody Map<String, String> body) {
        try {
            String name = body.get("name");
            String unit = body.getOrDefault("unit", "원");
            if (name == null || name.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "카테고리 이름을 입력해주세요."));
            if (categoryRepository.existsByName(name.trim()))
                return ResponseEntity.badRequest().body(Map.of("error", "이미 존재하는 카테고리입니다."));
            if (!unit.equals("원") && !unit.equals("L"))
                return ResponseEntity.badRequest().body(Map.of("error", "단위는 '원' 또는 'L'만 가능합니다."));
            SalesCategory c = new SalesCategory();
            setField(c, "name",   name.trim());
            setField(c, "unit",   unit);
            setField(c, "active", true);
            categoryRepository.save(c);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/categories/{id}")
    @Transactional
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            SalesCategory c = categoryRepository.findById(id).orElseThrow();
            setField(c, "active", false);
            categoryRepository.save(c);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 법인카드(지출) CRUD ──
    @GetMapping("/receipt")
    public ResponseEntity<?> getReceipts(@RequestParam String salesUsername, @RequestParam Integer year, @RequestParam Integer month) {
        var list = receiptRepository.findBySalesUsernameAndYearAndMonthOrderByDateAsc(salesUsername, year, month);
        return ResponseEntity.ok(list.stream().map(r -> buildReceiptMap(r)).toList());
    }

    @PostMapping("/receipt")
    public ResponseEntity<?> addReceipt(@RequestBody Map<String, Object> body) {
        try {
            String    salesUsername = (String) body.get("salesUsername");
            LocalDate date = LocalDate.parse((String) body.get("date"));
            String    unit = (String) body.getOrDefault("unit", "원");
            SalesReceipt r = new SalesReceipt();
            buildReceipt(r, body, salesUsername, date, unit);
            receiptRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    @PutMapping("/receipt/{id}")
    public ResponseEntity<?> updateReceipt(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            SalesReceipt r = receiptRepository.findById(id).orElseThrow();
            LocalDate date = LocalDate.parse((String) body.get("date"));
            String    unit = (String) body.getOrDefault("unit", "원");
            buildReceipt(r, body, r.getSalesUsername(), date, unit);
            receiptRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    @DeleteMapping("/receipt/{id}")
    public ResponseEntity<?> deleteReceipt(@PathVariable Long id) {
        receiptRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 운행내역 CRUD ──
    @GetMapping("/driving")
    public ResponseEntity<?> getDriving(@RequestParam String salesUsername, @RequestParam Integer year, @RequestParam Integer month) {
        var list = drivingRepository.findBySalesUsernameAndYearAndMonthOrderByIdAsc(salesUsername, year, month);
        return ResponseEntity.ok(list.stream().map(d -> Map.of(
                "id", d.getId(), "date", d.getDate() != null ? d.getDate().toString() : "",
                "totalFuelDetail", d.getTotalFuelDetail() != null ? d.getTotalFuelDetail() : "",
                "averageDistance", d.getAverageDistance() != null ? d.getAverageDistance() : 0.0,
                "totalFuelCost", d.getTotalFuelCost() != null ? d.getTotalFuelCost() : 0L
        )).toList());
    }

    @PostMapping("/driving")
    public ResponseEntity<?> addDriving(@RequestBody Map<String, Object> body) {
        try {
            String    salesUsername = (String) body.get("salesUsername");
            LocalDate date = LocalDate.parse((String) body.get("date"));
            SalesDriving d = new SalesDriving();
            setField(d, "salesUsername", salesUsername); setField(d, "drivingMonth", date.getMonthValue() + "월");
            setField(d, "date", date); setField(d, "totalFuelDetail", body.getOrDefault("totalFuelDetail", ""));
            setField(d, "averageDistance", parseD(body, "averageDistance")); setField(d, "totalFuelCost", parseL(body, "totalFuelCost"));
            setField(d, "year", date.getYear()); setField(d, "month", date.getMonthValue()); setField(d, "locked", false);
            drivingRepository.save(d);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    @PutMapping("/driving/{id}")
    public ResponseEntity<?> updateDriving(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            SalesDriving d = drivingRepository.findById(id).orElseThrow();
            LocalDate date = LocalDate.parse((String) body.get("date"));
            setField(d, "date", date); setField(d, "drivingMonth", date.getMonthValue() + "월");
            setField(d, "totalFuelDetail", body.getOrDefault("totalFuelDetail", ""));
            setField(d, "averageDistance", parseD(body, "averageDistance")); setField(d, "totalFuelCost", parseL(body, "totalFuelCost"));
            setField(d, "year", date.getYear()); setField(d, "month", date.getMonthValue());
            drivingRepository.save(d);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    @DeleteMapping("/driving/{id}")
    public ResponseEntity<?> deleteDriving(@PathVariable Long id) {
        drivingRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 공통 헬퍼 ──
    private void buildReceipt(SalesReceipt r, Map<String, Object> body, String salesUsername, LocalDate date, String unit) throws Exception {
        double amount = parseDouble(body, "amount");
        setField(r, "salesUsername",  salesUsername);
        setField(r, "date",           date);
        setField(r, "category",       body.getOrDefault("category", ""));
        setField(r, "unit",           unit);
        setField(r, "content",        body.getOrDefault("content", ""));
        setField(r, "amount",         amount);
        setField(r, "year",           date.getYear());
        setField(r, "month",          date.getMonthValue());
        setField(r, "locked",         false);

        if ("원".equals(unit)) {
            long total = (long) amount;
            setField(r, "totalAmount",    total);
            setField(r, "supplyAmount",   calcSupplyAmount(total));
            setField(r, "vat",            calcVat(total));
            setField(r, "businessNumber", body.getOrDefault("businessNumber", ""));
            setField(r, "companyName",    body.getOrDefault("companyName", ""));
        } else {
            // L 단위: amount=주유량(L), totalAmount=금액(원)
            long total = parseL(body, "totalAmount");
            setField(r, "totalAmount",    total);
            setField(r, "supplyAmount",   calcSupplyAmount(total));
            setField(r, "vat",            calcVat(total));
            setField(r, "businessNumber", body.getOrDefault("businessNumber", ""));
            setField(r, "companyName",    body.getOrDefault("companyName", ""));
        }
    }

    private Map<String, Object> buildReceiptMap(SalesReceipt r) {
        var m = new java.util.LinkedHashMap<String, Object>();
        m.put("id",             r.getId());
        m.put("date",           r.getDate().toString());
        m.put("category",       r.getCategory() != null ? r.getCategory() : "");
        m.put("unit",           r.getUnit() != null ? r.getUnit() : "원");
        m.put("content",        r.getContent() != null ? r.getContent() : "");
        m.put("amount",         r.getAmount() != null ? r.getAmount() : 0.0);
        m.put("totalAmount",    r.getTotalAmount() != null ? r.getTotalAmount() : 0L);
        m.put("supplyAmount",   r.getSupplyAmount() != null ? r.getSupplyAmount() : 0L);
        m.put("vat",            r.getVat() != null ? r.getVat() : 0L);
        m.put("businessNumber", r.getBusinessNumber() != null ? r.getBusinessNumber() : "");
        m.put("companyName",    r.getCompanyName() != null ? r.getCompanyName() : "");
        return m;
    }

    private long   parseL(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Long.parseLong(v.toString()) : 0L; }
    private double parseD(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Double.parseDouble(v.toString()) : 0.0; }
    private double parseDouble(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Double.parseDouble(v.toString()) : 0.0; }
    private void setField(Object obj, String fn, Object val) throws Exception { Field f = obj.getClass().getDeclaredField(fn); f.setAccessible(true); f.set(obj, val); }

    // ── SalesAdminController.java 에 추가할 내용 ──
// import 추가: SalesCash, SalesCashRepository
// 생성자에 추가: private final SalesCashRepository cashRepository;

    // ── 현금 조회 ──
    @GetMapping("/cash")
    public ResponseEntity<?> getCash(@RequestParam String salesUsername, @RequestParam Integer year, @RequestParam Integer month) {
        var list = cashRepository.findBySalesUsernameAndYearAndMonthOrderByDateAsc(salesUsername, year, month);
        return ResponseEntity.ok(list.stream().map(c -> buildCashMap(c)).toList());
    }

    // ── 현금 추가 ──
    @PostMapping("/cash")
    public ResponseEntity<?> addCash(@RequestBody Map<String, Object> body) {
        try {
            String    salesUsername = (String) body.get("salesUsername");
            LocalDate date = LocalDate.parse((String) body.get("date"));
            SalesCash c = new SalesCash();
            saveCash(c, body, salesUsername, date);
            cashRepository.save(c);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    // ── 현금 수정 ──
    @PutMapping("/cash/{id}")
    public ResponseEntity<?> updateCash(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            SalesCash c = cashRepository.findById(id).orElseThrow();
            LocalDate date = LocalDate.parse((String) body.get("date"));
            saveCash(c, body, c.getSalesUsername(), date);
            cashRepository.save(c);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    // ── 현금 삭제 ──
    @DeleteMapping("/cash/{id}")
    public ResponseEntity<?> deleteCash(@PathVariable Long id) {
        cashRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 헬퍼 ──
    private void saveCash(SalesCash c, Map<String, Object> body, String username, LocalDate date) throws Exception {
        String type   = (String) body.getOrDefault("type", "지출");
        String unit   = (String) body.getOrDefault("unit", "원");
        double amount = parseDouble(body, "amount");
        long   total  = "L".equals(unit) ? parseL(body, "totalAmount") : (long) parseDouble(body, "amount");

        setField(c, "salesUsername", username);
        setField(c, "date",         date);
        setField(c, "type",         type);
        setField(c, "paymentType",  "수입".equals(type) ? (String) body.getOrDefault("paymentType", "현금") : null);
        setField(c, "category",     body.getOrDefault("category", ""));
        setField(c, "unit",         unit);
        setField(c, "content",      body.getOrDefault("content", ""));
        setField(c, "amount",       "L".equals(unit) ? amount : (double) total);
        setField(c, "totalAmount",  total);
        setField(c, "supplyAmount", calcSupplyAmount(total));
        setField(c, "vat",          calcVat(total));
        setField(c, "companyName",  body.getOrDefault("companyName", ""));
        setField(c, "year",         date.getYear());
        setField(c, "month",        date.getMonthValue());
        setField(c, "locked",       false);
    }

    private Map<String, Object> buildCashMap(SalesCash c) {
        var m = new java.util.LinkedHashMap<String, Object>();
        m.put("id",          c.getId());
        m.put("date",        c.getDate().toString());
        m.put("type",        c.getType() != null ? c.getType() : "지출");
        m.put("paymentType", c.getPaymentType() != null ? c.getPaymentType() : "");
        m.put("category",    c.getCategory() != null ? c.getCategory() : "");
        m.put("unit",        c.getUnit() != null ? c.getUnit() : "원");
        m.put("content",     c.getContent() != null ? c.getContent() : "");
        m.put("amount",      c.getAmount() != null ? c.getAmount() : 0.0);
        m.put("totalAmount", c.getTotalAmount() != null ? c.getTotalAmount() : 0L);
        m.put("supplyAmount",c.getSupplyAmount() != null ? c.getSupplyAmount() : 0L);
        m.put("vat",         c.getVat() != null ? c.getVat() : 0L);
        m.put("companyName", c.getCompanyName() != null ? c.getCompanyName() : "");
        return m;
    }

}

