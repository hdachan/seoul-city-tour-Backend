package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.Category;
import com.example.seoulcitytour.entity.TourRecord;
import com.example.seoulcitytour.repository.CategoryRepository;
import com.example.seoulcitytour.repository.TourRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/record")
@PreAuthorize("@tabPermissionService.hasAccess(authentication, 'record')")
@RequiredArgsConstructor
public class RecordController {

    private final CategoryRepository   categoryRepository;
    private final TourRecordRepository tourRecordRepository;

    // ── 카테고리 ──
    @GetMapping("/categories/{type}")
    public ResponseEntity<?> getCategories(@PathVariable Integer type) {
        return ResponseEntity.ok(categoryRepository.findByTypeOrderByNameAsc(type).stream()
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "price", c.getPrice()))
                .toList());
    }

    @PostMapping("/categories")
    public ResponseEntity<?> addCategory(@RequestBody Map<String, Object> body) {
        try {
            Category c = new Category();
            setField(c, "type",  (Integer) body.get("type"));
            setField(c, "name",  (String)  body.get("name"));
            setField(c, "price", Long.valueOf(body.get("price").toString()));
            categoryRepository.save(c);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 운행기록 목록 (년/월 필터) ──
    @GetMapping("/list")
    public ResponseEntity<?> getRecords(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        List<TourRecord> list;

        if (year != null && month != null) {
            // 특정 년월 조회
            list = tourRecordRepository.findByYearAndMonth(year, month);
        } else {
            // 전체 조회 (통계 등에서 사용)
            list = tourRecordRepository.findAllByOrderByDateDesc();
        }

        return ResponseEntity.ok(list.stream().map(r -> Map.of(
                "id",        r.getId(),
                "date",      r.getDate().toString(),
                "category1", r.getCategory1() != null
                    ? Map.of("id", r.getCategory1().getId(), "name", r.getCategory1().getName(), "price", r.getCategory1().getPrice())
                    : Map.of(),
                "category2", r.getCategory2() != null
                    ? Map.of("id", r.getCategory2().getId(), "name", r.getCategory2().getName(), "price", r.getCategory2().getPrice())
                    : Map.of(),
                "count",  r.getCount(),
                "total",  r.getTotal(),
                "memo",   r.getMemo() != null ? r.getMemo() : ""
        )).toList());
    }

    // ── 운행기록 추가 ──
    @PostMapping("/add")
    public ResponseEntity<?> addRecord(@RequestBody Map<String, Object> body) {
        try {
            LocalDate date   = LocalDate.parse((String) body.get("date"));
            Integer count    = (Integer) body.get("count");
            String memo      = (String) body.getOrDefault("memo", "");
            Long category1Id = Long.valueOf(body.get("category1Id").toString());
            Long category2Id = Long.valueOf(body.get("category2Id").toString());

            Category cat1 = categoryRepository.findById(category1Id).orElseThrow();
            Category cat2 = categoryRepository.findById(category2Id).orElseThrow();
            long total = (cat1.getPrice() + cat2.getPrice()) * count;

            TourRecord record = new TourRecord();
            setField(record, "date",      date);
            setField(record, "category1", cat1);
            setField(record, "category2", cat2);
            setField(record, "count",     count);
            setField(record, "total",     total);
            setField(record, "memo",      memo);
            tourRecordRepository.save(record);
            return ResponseEntity.ok(Map.of("message", "기록이 추가되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage())); }
    }

    // ── 운행기록 수정 ──
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecord(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            TourRecord record = tourRecordRepository.findById(id).orElseThrow();
            LocalDate date   = LocalDate.parse((String) body.get("date"));
            Integer count    = (Integer) body.get("count");
            String memo      = (String) body.getOrDefault("memo", "");
            Long category1Id = Long.valueOf(body.get("category1Id").toString());
            Long category2Id = Long.valueOf(body.get("category2Id").toString());

            Category cat1 = categoryRepository.findById(category1Id).orElseThrow();
            Category cat2 = categoryRepository.findById(category2Id).orElseThrow();
            long total = (cat1.getPrice() + cat2.getPrice()) * count;

            setField(record, "date",      date);
            setField(record, "category1", cat1);
            setField(record, "category2", cat2);
            setField(record, "count",     count);
            setField(record, "total",     total);
            setField(record, "memo",      memo);
            tourRecordRepository.save(record);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage())); }
    }

    // ── 운행기록 삭제 ──
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id) {
        tourRecordRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 통계 ──
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestParam Integer year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end   = LocalDate.of(year, 12, 31);
        var records = tourRecordRepository.findByDateBetweenOrderByDateAsc(start, end);

        Map<Integer, Long> monthlyTotal = new LinkedHashMap<>();
        Map<Integer, Long> monthlyCount = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) { monthlyTotal.put(m, 0L); monthlyCount.put(m, 0L); }

        Map<String, Long> cat1Totals = new LinkedHashMap<>();
        Map<String, Long> cat2Totals = new LinkedHashMap<>();
        long grandTotal = 0, grandCount = 0;

        for (TourRecord r : records) {
            int m = r.getDate().getMonthValue();
            monthlyTotal.merge(m, r.getTotal(), Long::sum);
            monthlyCount.merge(m, (long) r.getCount(), Long::sum);
            grandTotal += r.getTotal();
            grandCount += r.getCount();
            if (r.getCategory1() != null) cat1Totals.merge(r.getCategory1().getName(), r.getTotal(), Long::sum);
            if (r.getCategory2() != null) cat2Totals.merge(r.getCategory2().getName(), r.getTotal(), Long::sum);
        }

        var monthlyChart = monthlyTotal.entrySet().stream()
                .map(e -> Map.of("month", e.getKey() + "월", "total", e.getValue(), "count", monthlyCount.get(e.getKey())))
                .toList();
        var cat1Chart = cat1Totals.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "value", e.getValue()))
                .sorted(Comparator.comparingLong(m -> -((Long)m.get("value")))).toList();
        var cat2Chart = cat2Totals.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "value", e.getValue()))
                .sorted(Comparator.comparingLong(m -> -((Long)m.get("value")))).toList();

        return ResponseEntity.ok(Map.of(
                "grandTotal", grandTotal, "grandCount", grandCount,
                "monthlyChart", monthlyChart, "cat1Chart", cat1Chart, "cat2Chart", cat2Chart
        ));
    }

    private void setField(Object obj, String fn, Object val) throws Exception {
        Field f = obj.getClass().getDeclaredField(fn);
        f.setAccessible(true);
        f.set(obj, val);
    }
}
