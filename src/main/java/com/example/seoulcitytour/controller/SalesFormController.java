package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.SalesDailyNote;
import com.example.seoulcitytour.entity.SalesDriving;
import com.example.seoulcitytour.entity.SalesMonthLock;
import com.example.seoulcitytour.entity.SalesReceipt;
import com.example.seoulcitytour.repository.SalesCategoryRepository;
import com.example.seoulcitytour.repository.SalesDailyNoteRepository;
import com.example.seoulcitytour.repository.SalesDrivingRepository;
import com.example.seoulcitytour.repository.SalesMonthLockRepository;
import com.example.seoulcitytour.repository.SalesReceiptRepository;
import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales-form")
@RequiredArgsConstructor
public class SalesFormController {

    private final SalesDrivingRepository   drivingRepository;
    private final SalesReceiptRepository   receiptRepository;
    private final SalesMonthLockRepository lockRepository;
    private final SalesCategoryRepository  categoryRepository;
    private final SalesDailyNoteRepository dailyNoteRepository;
    private final UserRepository           userRepository;

    private long calcSupplyAmount(long total) { return Math.round(total / 1.1); }
    private long calcVat(long total)          { return total - calcSupplyAmount(total); }

    private boolean isMonthLocked(String username, int year, int month) {
        return lockRepository.findBySalesUsernameAndYearAndMonth(username, year, month)
                .map(SalesMonthLock::getLocked).orElse(false);
    }

    // ── 잠금 상태 ──
    @GetMapping("/lock-status")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getLockStatus(@RequestParam Integer year,
                                           @RequestParam Integer month,
                                           Authentication auth) {
        return ResponseEntity.ok(Map.of("locked", isMonthLocked(auth.getName(), year, month)));
    }

    // ── 카테고리 ──
    @GetMapping("/categories")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "unit", c.getUnit()))
                .toList());
    }

    // ── 내 카드번호 ──
    @GetMapping("/my-card")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getMyCard(Authentication auth) {
        var user = userRepository.findByUsername(auth.getName()).orElse(null);
        return ResponseEntity.ok(Map.of("cardNumber",
                user != null && user.getCardNumber() != null ? user.getCardNumber() : ""));
    }

    // ── 용무 자동완성 ──
    @GetMapping("/purposes")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getPurposes(Authentication auth) {
        return ResponseEntity.ok(drivingRepository.findDistinctPurposes(auth.getName()));
    }

    // ────────────────────────────────────────
    // 일별 비고
    // ────────────────────────────────────────

    @GetMapping("/daily-note")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getDailyNote(@RequestParam String date, Authentication auth) {
        var note = dailyNoteRepository.findBySalesUsernameAndDate(auth.getName(), LocalDate.parse(date));
        return ResponseEntity.ok(Map.of("note", note.map(SalesDailyNote::getNote).orElse("")));
    }

    @PostMapping("/daily-note")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> saveDailyNote(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            LocalDate date = LocalDate.parse((String) body.get("date"));
            String    note = (String) body.getOrDefault("note", "");

            if (date.isBefore(LocalDate.now()))
                return ResponseEntity.badRequest().body(Map.of("error", "이전 날짜의 비고는 수정할 수 없습니다."));
            if (isMonthLocked(auth.getName(), date.getYear(), date.getMonthValue()))
                return ResponseEntity.badRequest().body(Map.of("error", "잠긴 달입니다."));

            SalesDailyNote n = dailyNoteRepository
                    .findBySalesUsernameAndDate(auth.getName(), date)
                    .orElse(new SalesDailyNote());
            setField(n, "salesUsername", auth.getName());
            setField(n, "date", date);
            setField(n, "note", note);
            dailyNoteRepository.save(n);
            return ResponseEntity.ok(Map.of("message", "저장되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/daily-notes")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getDailyNotes(@RequestParam Integer year,
                                           @RequestParam Integer month,
                                           Authentication auth) {
        YearMonth ym = YearMonth.of(year, month);
        var list = dailyNoteRepository.findBySalesUsernameAndDateBetween(
                auth.getName(), ym.atDay(1), ym.atEndOfMonth());
        return ResponseEntity.ok(list.stream()
                .map(n -> Map.of("date", n.getDate().toString(), "note", n.getNote()))
                .toList());
    }

    // ────────────────────────────────────────
    // 운행일지
    // ────────────────────────────────────────

    @GetMapping("/driving")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getDriving(@RequestParam Integer year,
                                        @RequestParam Integer month,
                                        Authentication auth) {
        var list = drivingRepository.findBySalesUsernameAndYearAndMonthOrderByDateAscIdAsc(
                auth.getName(), year, month);
        return ResponseEntity.ok(list.stream().map(d -> buildDrivingMap(d)).toList());
    }

    @GetMapping("/driving/date")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getDrivingByDate(@RequestParam String date, Authentication auth) {
        var list = drivingRepository.findBySalesUsernameAndDateOrderByIdAsc(
                auth.getName(), LocalDate.parse(date));
        return ResponseEntity.ok(list.stream().map(d -> buildDrivingMap(d)).toList());
    }

    @GetMapping("/driving/prev-meter")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getPrevMeter(@RequestParam String date, Authentication auth) {
        var list = drivingRepository.findPrevMeterReadings(auth.getName(), LocalDate.parse(date));
        Integer prevMeter = list.isEmpty() ? null : list.get(0).getMeterReading();
        return ResponseEntity.ok(Map.of("prevMeter", prevMeter != null ? prevMeter : 0));
    }

    // 운행일지 추가 - 날짜 범위 지원 (startDate ~ endDate 동일 기록 저장)
    @PostMapping("/driving")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> addDriving(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            LocalDate startDate = LocalDate.parse((String) body.get("startDate"));
            LocalDate endDate   = body.get("endDate") != null && !((String)body.get("endDate")).isBlank()
                    ? LocalDate.parse((String) body.get("endDate")) : startDate;

            if (endDate.isBefore(startDate))
                return ResponseEntity.badRequest().body(Map.of("error", "종료일이 시작일보다 이전입니다."));

            // 날짜 범위 순회하며 저장
            List<LocalDate> dates = new ArrayList<>();
            LocalDate cur = startDate;
            while (!cur.isAfter(endDate)) {
                dates.add(cur);
                cur = cur.plusDays(1);
            }

            for (LocalDate date : dates) {
                if (isMonthLocked(auth.getName(), date.getYear(), date.getMonthValue()))
                    continue; // 잠긴 달은 건너뜀
                SalesDriving d = new SalesDriving();
                saveDriving(d, body, auth.getName(), date);
                drivingRepository.save(d);
            }

            return ResponseEntity.ok(Map.of("message",
                    dates.size() == 1 ? "추가되었습니다." : dates.size() + "일 추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/driving/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> updateDriving(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           Authentication auth) {
        try {
            SalesDriving d = drivingRepository.findById(id).orElseThrow();
            if (!d.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "권한 없음"));

            LocalDate date = LocalDate.parse((String) body.getOrDefault("startDate",
                    body.getOrDefault("date", d.getDate().toString())));

            if (isMonthLocked(auth.getName(), date.getYear(), date.getMonthValue()))
                return ResponseEntity.badRequest().body(Map.of("error", "잠긴 달입니다."));

            saveDriving(d, body, auth.getName(), date);
            drivingRepository.save(d);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/driving/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> deleteDriving(@PathVariable Long id, Authentication auth) {
        try {
            SalesDriving d = drivingRepository.findById(id).orElseThrow();
            if (!d.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "권한 없음"));
            if (isMonthLocked(auth.getName(), d.getYear(), d.getMonth()))
                return ResponseEntity.badRequest().body(Map.of("error", "잠긴 달입니다."));
            drivingRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ────────────────────────────────────────
    // 법인카드
    // ────────────────────────────────────────

    @GetMapping("/receipt")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> getReceipts(@RequestParam Integer year,
                                         @RequestParam Integer month,
                                         Authentication auth) {
        var list = receiptRepository.findBySalesUsernameAndYearAndMonthOrderByDateAsc(
                auth.getName(), year, month);
        return ResponseEntity.ok(list.stream().map(r -> buildReceiptMap(r)).toList());
    }

    @PostMapping("/receipt")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> addReceipt(@RequestBody Map<String, Object> body, Authentication auth) {
        LocalDate date = LocalDate.parse((String) body.get("date"));
        if (isMonthLocked(auth.getName(), date.getYear(), date.getMonthValue()))
            return ResponseEntity.badRequest().body(Map.of("error", "잠긴 달입니다."));
        try {
            SalesReceipt r = new SalesReceipt();
            saveReceipt(r, body, auth.getName(), date);
            receiptRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/receipt/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> updateReceipt(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           Authentication auth) {
        try {
            SalesReceipt r = receiptRepository.findById(id).orElseThrow();
            if (!r.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "권한 없음"));
            LocalDate date = LocalDate.parse((String) body.get("date"));
            if (isMonthLocked(auth.getName(), date.getYear(), date.getMonthValue()))
                return ResponseEntity.badRequest().body(Map.of("error", "잠긴 달입니다."));
            saveReceipt(r, body, auth.getName(), date);
            receiptRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/receipt/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales')")
    public ResponseEntity<?> deleteReceipt(@PathVariable Long id, Authentication auth) {
        try {
            SalesReceipt r = receiptRepository.findById(id).orElseThrow();
            if (!r.getSalesUsername().equals(auth.getName()))
                return ResponseEntity.status(403).body(Map.of("error", "권한 없음"));
            if (isMonthLocked(auth.getName(), r.getYear(), r.getMonth()))
                return ResponseEntity.badRequest().body(Map.of("error", "잠긴 달입니다."));
            receiptRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ────────────────────────────────────────
    // 헬퍼
    // ────────────────────────────────────────

    private void saveDriving(SalesDriving d, Map<String, Object> body, String username, LocalDate date) throws Exception {
        String type = (String) body.getOrDefault("type", "업무");

        Integer meter = body.get("meterReading") != null && !body.get("meterReading").toString().isBlank()
                ? Integer.parseInt(body.get("meterReading").toString()) : null;

        setField(d, "salesUsername", username);
        setField(d, "date",         date);
        setField(d, "type",         type);
        setField(d, "year",         date.getYear());
        setField(d, "month",        date.getMonthValue());
        setField(d, "locked",       false);
        setField(d, "meterReading", meter);

        if ("업무".equals(type)) {
            setField(d, "destination",   body.getOrDefault("destination", ""));
            setField(d, "arrivalTime",   body.getOrDefault("arrivalTime", ""));
            setField(d, "purpose",       "");
            setField(d, "fuelAmount",    0.0);
            setField(d, "fuelCost",      0L);
            setField(d, "fuelUnitPrice", 0);
        } else if ("주유".equals(type)) {
            setField(d, "destination",   "");
            setField(d, "arrivalTime",   body.getOrDefault("arrivalTime", ""));
            setField(d, "purpose",       "");
            setField(d, "fuelAmount",    parseDouble(body, "fuelAmount"));
            setField(d, "fuelCost",      parseL(body, "fuelCost"));
            setField(d, "fuelUnitPrice", parseI(body, "fuelUnitPrice"));
        } else { // 휴가
            setField(d, "destination",   "");
            setField(d, "arrivalTime",   "");
            setField(d, "purpose",       body.getOrDefault("purpose", ""));
            setField(d, "fuelAmount",    parseDouble(body, "fuelAmount"));
            setField(d, "fuelCost",      parseL(body, "fuelCost"));
            setField(d, "fuelUnitPrice", parseI(body, "fuelUnitPrice"));
        }
    }

    private void saveReceipt(SalesReceipt r, Map<String, Object> body, String username, LocalDate date) throws Exception {
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