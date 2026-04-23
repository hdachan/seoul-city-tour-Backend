package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.GinsengRecord;
import com.example.seoulcitytour.entity.GinsengSetting;
import com.example.seoulcitytour.repository.GinsengRecordRepository;
import com.example.seoulcitytour.repository.GinsengSettingRepository;
import com.example.seoulcitytour.repository.UserRepository;
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

    private final GinsengRecordRepository recordRepository;
    private final GinsengSettingRepository settingRepository;
    private final UserRepository userRepository;

    // ── 인삼 단가 조회 ──
    @GetMapping("/price")
    public ResponseEntity<?> getPrice() {
        var setting = settingRepository.findById(1L)
                .orElse(null);
        long price = setting != null ? setting.getPricePerUnit() : 5000L;
        return ResponseEntity.ok(Map.of("pricePerUnit", price));
    }

    // ── 인삼 단가 수정 ──
    @PostMapping("/price")
    public ResponseEntity<?> setPrice(@RequestBody Map<String, Object> body) {
        try {
            long price = Long.parseLong(body.get("pricePerUnit").toString());
            GinsengSetting setting = settingRepository.findById(1L)
                    .orElse(new GinsengSetting());
            setField(setting, "id", 1L);
            setField(setting, "pricePerUnit", price);
            settingRepository.save(setting);
            return ResponseEntity.ok(Map.of("message", "단가가 저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "저장 실패: " + e.getMessage()));
        }
    }

    // ── 가이드 목록 조회 (ROLE_GUIDE 유저) ──
    @GetMapping("/guides")
    public ResponseEntity<?> getGuides() {
        var guides = userRepository.findAll().stream()
                .filter(u -> "ROLE_GUIDE".equals(u.getRole()))
                .map(u -> Map.of(
                        "id",   u.getId(),
                        "name", u.getName() != null ? u.getName() : u.getUsername()
                ))
                .toList();
        return ResponseEntity.ok(guides);
    }

    // ── 월별 기록 조회 ──
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthly(@RequestParam Integer year, @RequestParam Integer month) {
        YearMonth ym    = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();

        var records = recordRepository.findByDateBetweenOrderByGuideNameAsc(start, end);
        var result  = records.stream().map(r -> Map.of(
                "id",        r.getId(),
                "guideName", r.getGuideName(),
                "date",      r.getDate().toString(),
                "count",     r.getCount()
        )).toList();
        return ResponseEntity.ok(result);
    }

    // ── 기록 저장 (없으면 생성, 있으면 수정) ──
    @PostMapping("/save")
    public ResponseEntity<?> saveRecord(@RequestBody Map<String, Object> body) {
        try {
            String guideName = (String) body.get("guideName");
            String dateStr   = (String) body.get("date");
            Double count     = Double.parseDouble(body.get("count").toString());
            LocalDate date   = LocalDate.parse(dateStr);

            GinsengRecord record = recordRepository
                    .findByGuideNameAndDate(guideName, date)
                    .orElse(new GinsengRecord());

            setField(record, "guideName", guideName);
            setField(record, "date",      date);
            setField(record, "count",     count);
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
