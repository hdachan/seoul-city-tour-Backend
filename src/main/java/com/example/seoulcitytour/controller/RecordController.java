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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/record")
@PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
@RequiredArgsConstructor
public class RecordController {

    private final CategoryRepository categoryRepository;
    private final TourRecordRepository tourRecordRepository;

    // ── 카테고리 목록 조회 ──
    @GetMapping("/categories/{type}")
    public ResponseEntity<?> getCategories(@PathVariable Integer type) {
        List<Category> list = categoryRepository.findByTypeOrderByNameAsc(type);
        var result = list.stream().map(c -> Map.of(
                "id",    c.getId(),
                "name",  c.getName(),
                "price", c.getPrice()
        )).toList();
        return ResponseEntity.ok(result);
    }

    // ── 카테고리 추가 ──
    @PostMapping("/categories")
    public ResponseEntity<?> addCategory(@RequestBody Map<String, Object> body) {
        try {
            Integer type  = (Integer) body.get("type");
            String name   = (String)  body.get("name");
            Long price    = Long.valueOf(body.get("price").toString());

            Category c = new Category();
            setField(c, "type",  type);
            setField(c, "name",  name);
            setField(c, "price", price);
            categoryRepository.save(c);

            return ResponseEntity.ok(Map.of("message", "카테고리가 추가되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "카테고리 추가 실패: " + e.getMessage()));
        }
    }

    // ── 카테고리 삭제 ──
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "카테고리가 삭제되었습니다."));
    }

    // ── 운행 기록 목록 ──
    @GetMapping("/list")
    public ResponseEntity<?> getRecords() {
        var list = tourRecordRepository.findAllByOrderByDateDesc();
        var result = list.stream().map(r -> Map.of(
                "id",        r.getId(),
                "date",      r.getDate().toString(),
                "category1", r.getCategory1() != null ? Map.of("id", r.getCategory1().getId(), "name", r.getCategory1().getName(), "price", r.getCategory1().getPrice()) : Map.of(),
                "category2", r.getCategory2() != null ? Map.of("id", r.getCategory2().getId(), "name", r.getCategory2().getName(), "price", r.getCategory2().getPrice()) : Map.of(),
                "count",     r.getCount(),
                "total",     r.getTotal(),
                "memo",      r.getMemo() != null ? r.getMemo() : ""
        )).toList();
        return ResponseEntity.ok(result);
    }

    // ── 운행 기록 추가 ──
    @PostMapping("/add")
    public ResponseEntity<?> addRecord(@RequestBody Map<String, Object> body) {
        try {
            LocalDate date       = LocalDate.parse((String) body.get("date"));
            Integer count        = (Integer) body.get("count");
            String memo          = (String) body.getOrDefault("memo", "");
            Long category1Id     = Long.valueOf(body.get("category1Id").toString());
            Long category2Id     = Long.valueOf(body.get("category2Id").toString());

            Category cat1 = categoryRepository.findById(category1Id)
                    .orElseThrow(() -> new IllegalArgumentException("내역1 카테고리를 찾을 수 없습니다."));
            Category cat2 = categoryRepository.findById(category2Id)
                    .orElseThrow(() -> new IllegalArgumentException("내역2 카테고리를 찾을 수 없습니다."));

            // 토탈 계산: (cat1 가격 + cat2 가격) × 대수
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "기록 추가 실패: " + e.getMessage()));
        }
    }

    // ── 운행 기록 삭제 ──
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id) {
        tourRecordRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "기록이 삭제되었습니다."));
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
