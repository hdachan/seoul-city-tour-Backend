package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.SalesDriving;
import com.example.seoulcitytour.entity.SalesMonthLock;
import com.example.seoulcitytour.entity.SalesReceipt;
import com.example.seoulcitytour.repository.SalesDrivingRepository;
import com.example.seoulcitytour.repository.SalesMonthLockRepository;
import com.example.seoulcitytour.repository.SalesReceiptRepository;
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
@RequestMapping("/api/sales-form")
@RequiredArgsConstructor
public class SalesFormController {

    private final SalesReceiptRepository   receiptRepository;
    private final SalesDrivingRepository   drivingRepository;
    private final SalesMonthLockRepository lockRepository;

    private boolean isMonthLocked(String salesUsername) {
        LocalDate now = LocalDate.now();
        return lockRepository
                .findBySalesUsernameAndYearAndMonth(salesUsername, now.getYear(), now.getMonthValue())
                .map(SalesMonthLock::getLocked).orElse(false);
    }

    // 이번 달 검증
    private boolean isCurrentMonth(LocalDate date) {
        return YearMonth.from(date).equals(YearMonth.now());
    }

    // ── 잠금 상태 ──
    @GetMapping("/lock-status")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getLockStatus(Authentication auth) {
        return ResponseEntity.ok(Map.of("locked", isMonthLocked(auth.getName())));
    }

    // ─────────────────────────────────────
    // 수취금액 합계표
    // ─────────────────────────────────────

    @GetMapping("/receipt")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getReceipts(Authentication auth) {
        LocalDate now = LocalDate.now();
        var list = receiptRepository.findBySalesUsernameAndYearAndMonthOrderByDateAsc(
                auth.getName(), now.getYear(), now.getMonthValue());
        return ResponseEntity.ok(list.stream().map(r -> Map.of(
                "id",             r.getId(),
                "date",           r.getDate().toString(),
                "content",        r.getContent()        != null ? r.getContent()        : "",
                "totalAmount",    r.getTotalAmount()    != null ? r.getTotalAmount()    : 0L,
                "supplyAmount",   r.getSupplyAmount()   != null ? r.getSupplyAmount()   : 0L,
                "businessNumber", r.getBusinessNumber() != null ? r.getBusinessNumber() : "",
                "companyName",    r.getCompanyName()    != null ? r.getCompanyName()    : ""
        )).toList());
    }

    @PostMapping("/receipt")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> addReceipt(@RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (!isCurrentMonth(date))
                return ResponseEntity.badRequest().body(Map.of("error", "이번 달 날짜만 입력 가능합니다."));

            SalesReceipt r = new SalesReceipt();
            setField(r, "salesUsername",  auth.getName());
            setField(r, "date",           date);
            setField(r, "content",        body.getOrDefault("content", ""));
            setField(r, "totalAmount",    parseL(body, "totalAmount"));
            setField(r, "supplyAmount",   parseL(body, "supplyAmount"));
            setField(r, "businessNumber", body.getOrDefault("businessNumber", ""));
            setField(r, "companyName",    body.getOrDefault("companyName", ""));
            setField(r, "year",           date.getYear());
            setField(r, "month",          date.getMonthValue());
            setField(r, "locked",         false);
            receiptRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    @PutMapping("/receipt/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> updateReceipt(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            SalesReceipt r = receiptRepository.findById(id).orElseThrow();
            if (!r.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 수정 가능합니다."));
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (!isCurrentMonth(date))
                return ResponseEntity.badRequest().body(Map.of("error", "이번 달 날짜만 입력 가능합니다."));
            setField(r, "date",           date);
            setField(r, "content",        body.getOrDefault("content", ""));
            setField(r, "totalAmount",    parseL(body, "totalAmount"));
            setField(r, "supplyAmount",   parseL(body, "supplyAmount"));
            setField(r, "businessNumber", body.getOrDefault("businessNumber", ""));
            setField(r, "companyName",    body.getOrDefault("companyName", ""));
            receiptRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

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

    // ─────────────────────────────────────
    // 운행내역 (날짜 포함, 이번 달만)
    // ─────────────────────────────────────

    @GetMapping("/driving")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getDriving(Authentication auth) {
        LocalDate now = LocalDate.now();
        var list = drivingRepository.findBySalesUsernameAndYearAndMonthOrderByIdAsc(
                auth.getName(), now.getYear(), now.getMonthValue());
        return ResponseEntity.ok(list.stream().map(d -> Map.of(
                "id",              d.getId(),
                "date",            d.getDate() != null ? d.getDate().toString() : "",
                "totalFuelDetail", d.getTotalFuelDetail() != null ? d.getTotalFuelDetail() : "",
                "averageDistance", d.getAverageDistance() != null ? d.getAverageDistance() : 0.0,
                "totalFuelCost",   d.getTotalFuelCost()   != null ? d.getTotalFuelCost()   : 0L
        )).toList());
    }

    @PostMapping("/driving")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> addDriving(@RequestBody Map<String, Object> body, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (!isCurrentMonth(date))
                return ResponseEntity.badRequest().body(Map.of("error", "이번 달 날짜만 입력 가능합니다."));

            SalesDriving d = new SalesDriving();
            setField(d, "salesUsername",   auth.getName());
            setField(d, "drivingMonth",    date.getMonthValue() + "월"); // 자동 세팅
            setField(d, "date",            date);
            setField(d, "totalFuelDetail", body.getOrDefault("totalFuelDetail", ""));
            setField(d, "averageDistance", parseD(body, "averageDistance"));
            setField(d, "totalFuelCost",   parseL(body, "totalFuelCost"));
            setField(d, "year",            date.getYear());
            setField(d, "month",           date.getMonthValue());
            setField(d, "locked",          false);
            drivingRepository.save(d);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    @PutMapping("/driving/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> updateDriving(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            SalesDriving d = drivingRepository.findById(id).orElseThrow();
            if (!d.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 수정 가능합니다."));
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (!isCurrentMonth(date))
                return ResponseEntity.badRequest().body(Map.of("error", "이번 달 날짜만 입력 가능합니다."));
            setField(d, "date",            date);
            setField(d, "drivingMonth",    date.getMonthValue() + "월");
            setField(d, "totalFuelDetail", body.getOrDefault("totalFuelDetail", ""));
            setField(d, "averageDistance", parseD(body, "averageDistance"));
            setField(d, "totalFuelCost",   parseL(body, "totalFuelCost"));
            drivingRepository.save(d);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    @DeleteMapping("/driving/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> deleteDriving(@PathVariable Long id, Authentication auth) {
        if (isMonthLocked(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "이번 달은 잠겨있습니다."));
        try {
            SalesDriving d = drivingRepository.findById(id).orElseThrow();
            if (!d.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "본인 항목만 삭제 가능합니다."));
            drivingRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    private long   parseL(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Long.parseLong(v.toString()) : 0L; }
    private double parseD(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Double.parseDouble(v.toString()) : 0.0; }
    private void setField(Object obj, String fn, Object val) throws Exception { Field f = obj.getClass().getDeclaredField(fn); f.setAccessible(true); f.set(obj, val); }
}
