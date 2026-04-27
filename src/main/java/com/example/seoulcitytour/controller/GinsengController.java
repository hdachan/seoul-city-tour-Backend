package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.GinsengGuide;
import com.example.seoulcitytour.entity.GinsengRecord;
import com.example.seoulcitytour.entity.GinsengSetting;
import com.example.seoulcitytour.repository.GinsengGuideRepository;
import com.example.seoulcitytour.repository.GinsengRecordRepository;
import com.example.seoulcitytour.repository.GinsengSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/ginseng")
@PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
@RequiredArgsConstructor
public class GinsengController {

    private final GinsengRecordRepository  recordRepository;
    private final GinsengSettingRepository settingRepository;
    private final GinsengGuideRepository   guideRepository;

    // ── 인삼 단가 조회 ──
    @GetMapping("/price")
    public ResponseEntity<?> getPrice() {
        var setting = settingRepository.findById(1L).orElse(null);
        long price  = setting != null ? setting.getPricePerUnit() : 5000L;
        return ResponseEntity.ok(Map.of("pricePerUnit", price));
    }

    // ── 인삼 단가 수정 ──
    @PostMapping("/price")
    public ResponseEntity<?> setPrice(@RequestBody Map<String, Object> body) {
        try {
            long price = Long.parseLong(body.get("pricePerUnit").toString());
            GinsengSetting setting = settingRepository.findById(1L).orElse(new GinsengSetting());
            setField(setting, "id", 1L);
            setField(setting, "pricePerUnit", price);
            settingRepository.save(setting);
            return ResponseEntity.ok(Map.of("message", "단가가 저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "저장 실패"));
        }
    }

    // ── 가이드 목록 (활성만) ──
    @GetMapping("/guides")
    public ResponseEntity<?> getGuides() {
        var list = guideRepository.findByActiveTrueOrderByNameAsc();
        return ResponseEntity.ok(list.stream().map(g -> Map.of(
                "id", g.getId(), "name", g.getName(), "active", g.getActive()
        )).toList());
    }

    // ── 가이드 전체 목록 (비활성 포함) ──
    @GetMapping("/guides/all")
    public ResponseEntity<?> getAllGuides() {
        var list = guideRepository.findAllByOrderByActiveDescNameAsc();
        return ResponseEntity.ok(list.stream().map(g -> Map.of(
                "id", g.getId(), "name", g.getName(), "active", g.getActive()
        )).toList());
    }

    // ── 가이드 추가 ──
    @PostMapping("/guides")
    public ResponseEntity<?> addGuide(@RequestBody Map<String, String> body) {
        try {
            String name = body.get("name");
            if (name == null || name.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "이름을 입력해주세요."));
            GinsengGuide guide = new GinsengGuide();
            setField(guide, "name",   name.trim());
            setField(guide, "active", true);
            guideRepository.save(guide);
            return ResponseEntity.ok(Map.of("message", "가이드가 추가되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage()));
        }
    }

    // ── 가이드 활성/비활성 토글 ──
    @PostMapping("/guides/{id}/toggle")
    public ResponseEntity<?> toggleGuide(@PathVariable Long id) {
        try {
            GinsengGuide guide = guideRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("가이드를 찾을 수 없습니다."));
            setField(guide, "active", !guide.getActive());
            guideRepository.save(guide);
            String status = guide.getActive() ? "활성화" : "비활성화";
            return ResponseEntity.ok(Map.of("message", guide.getName() + "이(가) " + status + "되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── 월별 기록 조회 ──
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthly(@RequestParam Integer year, @RequestParam Integer month) {
        YearMonth ym  = YearMonth.of(year, month);
        var records   = recordRepository.findByDateBetweenOrderByGuideNameAsc(ym.atDay(1), ym.atEndOfMonth());
        var result    = records.stream().map(r -> Map.of(
                "id",            r.getId(),
                "guideName",     r.getGuideName(),
                "date",          r.getDate().toString(),
                "count",         r.getCount(),
                "priceSnapshot", r.getPriceSnapshot()  // 저장 시점 단가
        )).toList();
        return ResponseEntity.ok(result);
    }

    // ── 기록 저장 ──
    @PostMapping("/save")
    public ResponseEntity<?> saveRecord(@RequestBody Map<String, Object> body) {
        try {
            String    guideName = (String) body.get("guideName");
            LocalDate date      = LocalDate.parse((String) body.get("date"));
            Double    count     = Double.parseDouble(body.get("count").toString());

            // 현재 단가 조회 (스냅샷으로 저장)
            long price = settingRepository.findById(1L)
                    .map(GinsengSetting::getPricePerUnit).orElse(5000L);

            GinsengRecord record = recordRepository
                    .findByGuideNameAndDate(guideName, date)
                    .orElse(new GinsengRecord());

            setField(record, "guideName",      guideName);
            setField(record, "date",           date);
            setField(record, "count",          count);
            // 기존 기록이 없을 때만 현재 단가 저장 (기존 기록 수정 시엔 단가 유지)
            if (record.getId() == null) {
                setField(record, "priceSnapshot", price);
            }
            recordRepository.save(record);

            return ResponseEntity.ok(Map.of("message", "저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "저장 실패: " + e.getMessage()));
        }
    }

    // ── 기록 삭제 ──
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id) {
        recordRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
