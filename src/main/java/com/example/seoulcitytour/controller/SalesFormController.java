package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.SalesCash;
import com.example.seoulcitytour.entity.SalesDriving;
import com.example.seoulcitytour.entity.SalesMonthLock;
import com.example.seoulcitytour.entity.SalesReceipt;
import com.example.seoulcitytour.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-form")
@RequiredArgsConstructor
public class SalesFormController {

    private final SalesReceiptRepository   receiptRepository;
    private final SalesDrivingRepository   drivingRepository;
    private final SalesMonthLockRepository lockRepository;
    private final SalesCategoryRepository  categoryRepository;
    private final SalesCashRepository cashRepository;   // ← 이거 추가

    private long calcSupplyAmount(long total) { return Math.round(total / 1.1); }
    private long calcVat(long total)          { return total - calcSupplyAmount(total); }

    private boolean isMonthLocked(String salesUsername) {
        LocalDate now = LocalDate.now();
        return lockRepository.findBySalesUsernameAndYearAndMonth(salesUsername, now.getYear(), now.getMonthValue())
                .map(SalesMonthLock::getLocked).orElse(false);
    }

    private boolean isCurrentWeek(LocalDate date) {
        LocalDate today  = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        return !date.isBefore(monday) && !date.isAfter(sunday);
    }

    // ── 잠금 상태 ──
    @GetMapping("/lock-status")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getLockStatus(Authentication auth) {
        return ResponseEntity.ok(Map.of("locked", isMonthLocked(auth.getName())));
    }

    // ── 카테고리 목록 (unit 포함) ──
    @GetMapping("/categories")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "unit", c.getUnit()))
                .toList());
    }

    // ── 법인카드(지출) 조회 ──
    @GetMapping("/receipt")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getReceipts(Authentication auth) {
        LocalDate now = LocalDate.now();
        var list = receiptRepository.findBySalesUsernameAndYearAndMonthOrderByDateAsc(
                auth.getName(), now.getYear(), now.getMonthValue());
        return ResponseEntity.ok(list.stream().map(r -> {
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
        }).toList());
    }

    // ── 법인카드 추가 ──
    @PostMapping("/receipt")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> addReceipt(@RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (!isCurrentWeek(date))
                return ResponseEntity.badRequest().body(Map.of("error", "이번 주 날짜만 입력 가능합니다."));

            String unit   = (String) body.getOrDefault("unit", "원");
            double amount = parseDouble(body, "amount");

            SalesReceipt r = new SalesReceipt();
            setField(r, "salesUsername",  auth.getName());
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
            receiptRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    // ── 법인카드 수정 ──
    @PutMapping("/receipt/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> updateReceipt(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            SalesReceipt r = receiptRepository.findById(id).orElseThrow();
            if (!r.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 수정 가능합니다."));
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (!isCurrentWeek(date))
                return ResponseEntity.badRequest().body(Map.of("error", "이번 주 날짜만 입력 가능합니다."));

            String unit   = (String) body.getOrDefault("unit", "원");
            double amount = parseDouble(body, "amount");
            setField(r, "date", date); setField(r, "category", body.getOrDefault("category", ""));
            setField(r, "unit", unit); setField(r, "content",  body.getOrDefault("content", ""));
            setField(r, "amount", amount);

            if ("원".equals(unit)) {
                long total = (long) amount;
                setField(r, "totalAmount", total); setField(r, "supplyAmount", calcSupplyAmount(total)); setField(r, "vat", calcVat(total));
                setField(r, "businessNumber", body.getOrDefault("businessNumber", "")); setField(r, "companyName", body.getOrDefault("companyName", ""));
            } else {
                long total = parseL(body, "totalAmount");
                setField(r, "totalAmount", total); setField(r, "supplyAmount", calcSupplyAmount(total)); setField(r, "vat", calcVat(total));
                setField(r, "businessNumber", body.getOrDefault("businessNumber", "")); setField(r, "companyName", body.getOrDefault("companyName", ""));
            }
            receiptRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    // ── 법인카드 삭제 ──
    @DeleteMapping("/receipt/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> deleteReceipt(@PathVariable Long id, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            SalesReceipt r = receiptRepository.findById(id).orElseThrow();
            if (!r.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 삭제 가능합니다."));
            receiptRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 운행내역 ──
    @GetMapping("/driving")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getDriving(Authentication auth) {
        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(drivingRepository.findBySalesUsernameAndYearAndMonthOrderByIdAsc(
                auth.getName(), now.getYear(), now.getMonthValue()).stream().map(d -> Map.of(
                "id", d.getId(), "date", d.getDate() != null ? d.getDate().toString() : "",
                "totalFuelDetail", d.getTotalFuelDetail() != null ? d.getTotalFuelDetail() : "",
                "averageDistance", d.getAverageDistance() != null ? d.getAverageDistance() : 0.0,
                "totalFuelCost", d.getTotalFuelCost() != null ? d.getTotalFuelCost() : 0L
        )).toList());
    }

    @PostMapping("/driving")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> addDriving(@RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName())) return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (!isCurrentWeek(date)) return ResponseEntity.badRequest().body(Map.of("error", "이번 주 날짜만 입력 가능합니다."));
            SalesDriving d = new SalesDriving();
            setField(d, "salesUsername", auth.getName()); setField(d, "drivingMonth", date.getMonthValue() + "월");
            setField(d, "date", date); setField(d, "totalFuelDetail", body.getOrDefault("totalFuelDetail", ""));
            setField(d, "averageDistance", parseD(body, "averageDistance")); setField(d, "totalFuelCost", parseL(body, "totalFuelCost"));
            setField(d, "year", date.getYear()); setField(d, "month", date.getMonthValue()); setField(d, "locked", false);
            drivingRepository.save(d);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    @PutMapping("/driving/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> updateDriving(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName())) return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            SalesDriving d = drivingRepository.findById(id).orElseThrow();
            if (!d.getSalesUsername().equals(auth.getName())) return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 수정 가능합니다."));
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (!isCurrentWeek(date)) return ResponseEntity.badRequest().body(Map.of("error", "이번 주 날짜만 입력 가능합니다."));
            setField(d, "date", date); setField(d, "drivingMonth", date.getMonthValue() + "월");
            setField(d, "totalFuelDetail", body.getOrDefault("totalFuelDetail", ""));
            setField(d, "averageDistance", parseD(body, "averageDistance")); setField(d, "totalFuelCost", parseL(body, "totalFuelCost"));
            drivingRepository.save(d);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    @DeleteMapping("/driving/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> deleteDriving(@PathVariable Long id, Authentication auth) {
        if (isMonthLocked(auth.getName())) return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            SalesDriving d = drivingRepository.findById(id).orElseThrow();
            if (!d.getSalesUsername().equals(auth.getName())) return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 삭제 가능합니다."));
            drivingRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    private long   parseL(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Long.parseLong(v.toString()) : 0L; }
    private double parseD(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Double.parseDouble(v.toString()) : 0.0; }
    private double parseDouble(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Double.parseDouble(v.toString()) : 0.0; }
    private void setField(Object obj, String fn, Object val) throws Exception { Field f = obj.getClass().getDeclaredField(fn); f.setAccessible(true); f.set(obj, val); }

    // ── SalesFormController.java 에 추가할 내용 ──
// 클래스 상단 import 추가:
// import com.example.seoulcitytour.entity.SalesCash;
// import com.example.seoulcitytour.repository.SalesCashRepository;
// 생성자에 추가: private final SalesCashRepository cashRepository;

    // ── 현금 조회 ──
    @GetMapping("/cash")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getCash(Authentication auth) {
        LocalDate now = LocalDate.now();
        var list = cashRepository.findBySalesUsernameAndYearAndMonthOrderByDateAsc(
                auth.getName(), now.getYear(), now.getMonthValue());
        return ResponseEntity.ok(list.stream().map(c -> buildCashMap(c)).toList());
    }

    // ── 현금 추가 ──
    @PostMapping("/cash")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> addCash(@RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (!isCurrentWeek(date))
                return ResponseEntity.badRequest().body(Map.of("error", "이번 주 날짜만 입력 가능합니다."));
            SalesCash c = new SalesCash();
            saveCash(c, body, auth.getName(), date);
            cashRepository.save(c);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    // ── 현금 수정 ──
    @PutMapping("/cash/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> updateCash(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            SalesCash c = cashRepository.findById(id).orElseThrow();
            if (!c.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 수정 가능합니다."));
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (!isCurrentWeek(date))
                return ResponseEntity.badRequest().body(Map.of("error", "이번 주 날짜만 입력 가능합니다."));
            saveCash(c, body, auth.getName(), date);
            cashRepository.save(c);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    // ── 현금 삭제 ──
    @DeleteMapping("/cash/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> deleteCash(@PathVariable Long id, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            SalesCash c = cashRepository.findById(id).orElseThrow();
            if (!c.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 삭제 가능합니다."));
            cashRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 현금 헬퍼 ──
    private void saveCash(SalesCash c, Map<String, Object> body, String username, LocalDate date) throws Exception {
        String type    = (String) body.getOrDefault("type", "지출");
        String unit    = (String) body.getOrDefault("unit", "원");
        double amount  = parseDouble(body, "amount");
        long   total   = "L".equals(unit) ? parseL(body, "totalAmount") : (long) parseDouble(body, "amount");

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