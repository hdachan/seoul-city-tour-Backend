package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.SalesDriving;
import com.example.seoulcitytour.entity.SalesMonthLock;
import com.example.seoulcitytour.entity.SalesReceipt;
import com.example.seoulcitytour.repository.SalesDrivingRepository;
import com.example.seoulcitytour.repository.SalesMonthLockRepository;
import com.example.seoulcitytour.repository.SalesReceiptRepository;
import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // ── 영업 계정 목록 ──
    @GetMapping("/sales-users")
    public ResponseEntity<?> getSalesUsers() {
        return ResponseEntity.ok(userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> "ROLE_SALES".equals(u.getRole()))
                .map(u -> Map.of(
                        "username", u.getUsername(),
                        "name",     u.getName() != null ? u.getName() : u.getUsername()
                )).toList());
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
            return Map.of(
                    "username",      u.getUsername(),
                    "name",          u.getName() != null ? u.getName() : u.getUsername(),
                    "receiptCount",  receiptCount,
                    "drivingCount",  drivingCount,
                    "hasData",       (receiptCount + drivingCount) > 0,
                    "locked",        locked
            );
        }).toList());
    }

    // ── 잠금 상태 / 토글 ──
    @GetMapping("/lock-status")
    public ResponseEntity<?> getLockStatus(@RequestParam String salesUsername,
                                           @RequestParam Integer year, @RequestParam Integer month) {
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
            SalesMonthLock lock = lockRepository
                    .findBySalesUsernameAndYearAndMonth(salesUsername, year, month)
                    .orElse(new SalesMonthLock());
            setField(lock, "salesUsername", salesUsername);
            setField(lock, "year",          year);
            setField(lock, "month",         month);
            setField(lock, "locked",        locked);
            lockRepository.save(lock);
            return ResponseEntity.ok(Map.of("locked", locked));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 수취금액 CRUD ──
    @GetMapping("/receipt")
    public ResponseEntity<?> getReceipts(@RequestParam String salesUsername,
                                         @RequestParam Integer year, @RequestParam Integer month) {
        var list = receiptRepository.findBySalesUsernameAndYearAndMonthOrderByDateAsc(salesUsername, year, month);
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
    public ResponseEntity<?> addReceipt(@RequestBody Map<String, Object> body) {
        try {
            String    salesUsername = (String) body.get("salesUsername");
            LocalDate date = LocalDate.parse((String) body.get("date"));
            SalesReceipt r = new SalesReceipt();
            setField(r, "salesUsername",  salesUsername);
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
    public ResponseEntity<?> updateReceipt(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            SalesReceipt r = receiptRepository.findById(id).orElseThrow();
            LocalDate date = LocalDate.parse((String) body.get("date"));
            setField(r, "date",           date);
            setField(r, "content",        body.getOrDefault("content", ""));
            setField(r, "totalAmount",    parseL(body, "totalAmount"));
            setField(r, "supplyAmount",   parseL(body, "supplyAmount"));
            setField(r, "businessNumber", body.getOrDefault("businessNumber", ""));
            setField(r, "companyName",    body.getOrDefault("companyName", ""));
            setField(r, "year",           date.getYear());
            setField(r, "month",          date.getMonthValue());
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
    public ResponseEntity<?> getDriving(@RequestParam String salesUsername,
                                        @RequestParam Integer year, @RequestParam Integer month) {
        var list = drivingRepository.findBySalesUsernameAndYearAndMonthOrderByIdAsc(salesUsername, year, month);
        return ResponseEntity.ok(list.stream().map(d -> Map.of(
                "id",              d.getId(),
                "date",            d.getDate() != null ? d.getDate().toString() : "",
                "totalFuelDetail", d.getTotalFuelDetail() != null ? d.getTotalFuelDetail() : "",
                "averageDistance", d.getAverageDistance() != null ? d.getAverageDistance() : 0.0,
                "totalFuelCost",   d.getTotalFuelCost()   != null ? d.getTotalFuelCost()   : 0L
        )).toList());
    }

    @PostMapping("/driving")
    public ResponseEntity<?> addDriving(@RequestBody Map<String, Object> body) {
        try {
            String    salesUsername = (String) body.get("salesUsername");
            LocalDate date = LocalDate.parse((String) body.get("date"));
            SalesDriving d = new SalesDriving();
            setField(d, "salesUsername",   salesUsername);
            setField(d, "drivingMonth",    date.getMonthValue() + "월"); // 날짜에서 자동 세팅
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
    public ResponseEntity<?> updateDriving(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            SalesDriving d = drivingRepository.findById(id).orElseThrow();
            LocalDate date = LocalDate.parse((String) body.get("date"));
            setField(d, "date",            date);
            setField(d, "drivingMonth",    date.getMonthValue() + "월");
            setField(d, "totalFuelDetail", body.getOrDefault("totalFuelDetail", ""));
            setField(d, "averageDistance", parseD(body, "averageDistance"));
            setField(d, "totalFuelCost",   parseL(body, "totalFuelCost"));
            setField(d, "year",            date.getYear());
            setField(d, "month",           date.getMonthValue());
            drivingRepository.save(d);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    @DeleteMapping("/driving/{id}")
    public ResponseEntity<?> deleteDriving(@PathVariable Long id) {
        drivingRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    private long   parseL(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Long.parseLong(v.toString()) : 0L; }
    private double parseD(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Double.parseDouble(v.toString()) : 0.0; }
    private void setField(Object obj, String fn, Object val) throws Exception { Field f = obj.getClass().getDeclaredField(fn); f.setAccessible(true); f.set(obj, val); }
}
