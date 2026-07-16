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
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-admin")
@PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales-admin')")
@RequiredArgsConstructor
public class SalesAdminController {

    private final UserRepository            userRepository;
    private final SalesReceiptRepository    receiptRepository;
    private final SalesDrivingRepository    drivingRepository;
    private final SalesMonthLockRepository  lockRepository;
    private final SalesCategoryRepository   categoryRepository;
    private final SalesDailyNoteRepository  dailyNoteRepository;

    private long calcSupplyAmount(long total) { return Math.round(total / 1.1); }
    private long calcVat(long total)          { return total - calcSupplyAmount(total); }

    @GetMapping("/sales-users")
    public ResponseEntity<?> getSalesUsers() {
        return ResponseEntity.ok(userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> "ROLE_SALES".equals(u.getRole()))
                .map(u -> Map.of(
                        "username",   u.getUsername(),
                        "name",       u.getName() != null ? u.getName() : u.getUsername(),
                        "cardNumber", u.getCardNumber() != null ? u.getCardNumber() : ""
                )).toList());
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam Integer year, @RequestParam Integer month) {
        var users = userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> "ROLE_SALES".equals(u.getRole())).toList();
        return ResponseEntity.ok(users.stream().map(u -> {
            int receiptCount = receiptRepository.findBySalesUsernameAndYearAndMonthOrderByDateAsc(u.getUsername(), year, month).size();
            int drivingCount = drivingRepository.findBySalesUsernameAndYearAndMonthOrderByDateAscIdAsc(u.getUsername(), year, month).size();
            boolean locked   = lockRepository.findBySalesUsernameAndYearAndMonth(u.getUsername(), year, month)
                    .map(SalesMonthLock::getLocked).orElse(false);
            return Map.of(
                    "username",     u.getUsername(),
                    "name",         u.getName() != null ? u.getName() : u.getUsername(),
                    "receiptCount", receiptCount,
                    "drivingCount", drivingCount,
                    "hasData",      (receiptCount + drivingCount) > 0,
                    "locked",       locked
            );
        }).toList());
    }

    @GetMapping("/lock-status")
    public ResponseEntity<?> getLockStatus(@RequestParam String salesUsername,
                                           @RequestParam Integer year,
                                           @RequestParam Integer month) {
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
            setField(lock, "year", year);
            setField(lock, "month", month);
            setField(lock, "locked", locked);
            lockRepository.save(lock);
            return ResponseEntity.ok(Map.of("locked", locked));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 카테고리 ──
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
            SalesCategory c = new SalesCategory();
            setField(c, "name", name.trim());
            setField(c, "unit", unit);
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

    // ── 운행일지 ──
    @GetMapping("/driving")
    public ResponseEntity<?> getDriving(@RequestParam String salesUsername,
                                        @RequestParam Integer year,
                                        @RequestParam Integer month) {
        var list = drivingRepository.findBySalesUsernameAndYearAndMonthOrderByDateAscIdAsc(salesUsername, year, month);
        return ResponseEntity.ok(list.stream().map(d -> buildDrivingMap(d)).toList());
    }

    @GetMapping("/driving/date")
    public ResponseEntity<?> getDrivingByDate(@RequestParam String salesUsername,
                                              @RequestParam String date) {
        var list = drivingRepository.findBySalesUsernameAndDateOrderByIdAsc(salesUsername, LocalDate.parse(date));
        return ResponseEntity.ok(list.stream().map(d -> buildDrivingMap(d)).toList());
    }

    @PostMapping("/driving")
    public ResponseEntity<?> addDriving(@RequestBody Map<String, Object> body) {
        try {
            String    salesUsername = (String) body.get("salesUsername");
            LocalDate date = LocalDate.parse((String) body.get("date"));
            SalesDriving d = new SalesDriving();
            saveDriving(d, body, salesUsername, date);
            drivingRepository.save(d);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    @PutMapping("/driving/{id}")
    public ResponseEntity<?> updateDriving(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        try {
            SalesDriving d = drivingRepository.findById(id).orElseThrow();
            LocalDate date = LocalDate.parse((String) body.get("date"));
            saveDriving(d, body, d.getSalesUsername(), date);
            drivingRepository.save(d);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    @DeleteMapping("/driving/{id}")
    public ResponseEntity<?> deleteDriving(@PathVariable Long id) {
        drivingRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 법인카드 ──
    @GetMapping("/receipt")
    public ResponseEntity<?> getReceipts(@RequestParam String salesUsername,
                                         @RequestParam Integer year,
                                         @RequestParam Integer month) {
        var list = receiptRepository.findBySalesUsernameAndYearAndMonthOrderByDateAsc(salesUsername, year, month);
        return ResponseEntity.ok(list.stream().map(r -> buildReceiptMap(r)).toList());
    }

    @PostMapping("/receipt")
    public ResponseEntity<?> addReceipt(@RequestBody Map<String, Object> body) {
        try {
            String    salesUsername = (String) body.get("salesUsername");
            LocalDate date = LocalDate.parse((String) body.get("date"));
            SalesReceipt r = new SalesReceipt();
            buildReceipt(r, body, salesUsername, date);
            receiptRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    @PutMapping("/receipt/{id}")
    public ResponseEntity<?> updateReceipt(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        try {
            SalesReceipt r = receiptRepository.findById(id).orElseThrow();
            LocalDate date = LocalDate.parse((String) body.get("date"));
            buildReceipt(r, body, r.getSalesUsername(), date);
            receiptRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    @DeleteMapping("/receipt/{id}")
    public ResponseEntity<?> deleteReceipt(@PathVariable Long id) {
        receiptRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 일별 비고 ──
    @GetMapping("/daily-note")
    public ResponseEntity<?> getDailyNote(@RequestParam String salesUsername,
                                          @RequestParam String date) {
        var note = dailyNoteRepository.findBySalesUsernameAndDate(salesUsername, LocalDate.parse(date));
        return ResponseEntity.ok(Map.of("note", note.map(SalesDailyNote::getNote).orElse("")));
    }

    @GetMapping("/daily-notes")
    public ResponseEntity<?> getDailyNotes(@RequestParam String salesUsername,
                                           @RequestParam Integer year,
                                           @RequestParam Integer month) {
        YearMonth ym = YearMonth.of(year, month);
        var list = dailyNoteRepository.findBySalesUsernameAndDateBetween(
                salesUsername, ym.atDay(1), ym.atEndOfMonth());
        return ResponseEntity.ok(list.stream()
                .map(n -> Map.of("date", n.getDate().toString(), "note", n.getNote()))
                .toList());
    }

    @PostMapping("/daily-note")
    public ResponseEntity<?> saveDailyNote(@RequestBody Map<String, Object> body) {
        try {
            String    salesUsername = (String) body.get("salesUsername");
            LocalDate date = LocalDate.parse((String) body.get("date"));
            String    note = (String) body.getOrDefault("note", "");
            SalesDailyNote n = dailyNoteRepository
                    .findBySalesUsernameAndDate(salesUsername, date)
                    .orElse(new SalesDailyNote());
            setField(n, "salesUsername", salesUsername);
            setField(n, "date", date);
            setField(n, "note", note);
            dailyNoteRepository.save(n);
            return ResponseEntity.ok(Map.of("message", "저장되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 헬퍼 ──
    private void saveDriving(SalesDriving d, Map<String, Object> body, String username, LocalDate date) throws Exception {
        Integer meter = body.get("meterReading") != null && !body.get("meterReading").toString().isBlank()
                ? Integer.parseInt(body.get("meterReading").toString()) : null;
        setField(d, "salesUsername", username);
        setField(d, "date",          date);
        setField(d, "type",          body.getOrDefault("type", "업무"));
        setField(d, "destination",   body.getOrDefault("destination", ""));
        setField(d, "arrivalTime",   body.getOrDefault("arrivalTime", ""));
        setField(d, "meterReading",  meter);
        setField(d, "purpose",       body.getOrDefault("purpose", ""));
        setField(d, "fuelAmount",    parseDouble(body, "fuelAmount"));
        setField(d, "fuelCost",      parseL(body, "fuelCost"));
        setField(d, "fuelUnitPrice", parseI(body, "fuelUnitPrice"));
        setField(d, "note",          body.getOrDefault("note", ""));
        setField(d, "year",          date.getYear());
        setField(d, "month",         date.getMonthValue());
        setField(d, "locked",        false);
    }

    private void buildReceipt(SalesReceipt r, Map<String, Object> body, String username, LocalDate date) throws Exception {
        double amount = parseDouble(body, "amount");
        long   total  = (long) amount;
        setField(r, "salesUsername",  username);
        setField(r, "date",           date);
        setField(r, "category",       body.getOrDefault("category", ""));
        setField(r, "unit",           "원");
        setField(r, "content",        body.getOrDefault("content", ""));
        setField(r, "amount",         amount);
        setField(r, "totalAmount",    total);
        setField(r, "supplyAmount",   calcSupplyAmount(total));
        setField(r, "vat",            calcVat(total));
        setField(r, "businessNumber", body.getOrDefault("businessNumber", ""));
        setField(r, "companyName",    body.getOrDefault("companyName", ""));
        setField(r, "year",           date.getYear());
        setField(r, "month",          date.getMonthValue());
        setField(r, "locked",         false);
    }

    private Map<String, Object> buildDrivingMap(SalesDriving d) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id",            d.getId());
        m.put("date",          d.getDate().toString());
        m.put("type",          d.getType() != null ? d.getType() : "업무");
        m.put("destination",   d.getDestination() != null ? d.getDestination() : "");
        m.put("arrivalTime",   d.getArrivalTime() != null ? d.getArrivalTime() : "");
        m.put("meterReading",  d.getMeterReading() != null ? d.getMeterReading() : 0);
        m.put("purpose",       d.getPurpose() != null ? d.getPurpose() : "");
        m.put("fuelAmount",    d.getFuelAmount() != null ? d.getFuelAmount() : 0.0);
        m.put("fuelCost",      d.getFuelCost() != null ? d.getFuelCost() : 0L);
        m.put("fuelUnitPrice", d.getFuelUnitPrice() != null ? d.getFuelUnitPrice() : 0);
        m.put("note",          d.getNote() != null ? d.getNote() : "");
        return m;
    }

    private Map<String, Object> buildReceiptMap(SalesReceipt r) {
        var m = new LinkedHashMap<String, Object>();
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

    private double parseDouble(Map<String, Object> b, String k) { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Double.parseDouble(v.toString()) : 0.0; }
    private long   parseL(Map<String, Object> b, String k)      { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Long.parseLong(v.toString()) : 0L; }
    private int    parseI(Map<String, Object> b, String k)      { Object v = b.get(k); return (v != null && !v.toString().isBlank()) ? Integer.parseInt(v.toString()) : 0; }
    private void setField(Object obj, String fn, Object val) throws Exception { Field f = obj.getClass().getDeclaredField(fn); f.setAccessible(true); f.set(obj, val); }
}