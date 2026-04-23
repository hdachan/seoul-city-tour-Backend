package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.Platform;
import com.example.seoulcitytour.entity.PlatformSettlement;
import com.example.seoulcitytour.repository.PlatformRepository;
import com.example.seoulcitytour.repository.PlatformSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.Map;

@RestController
@RequestMapping("/api/settlement")
@PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
@RequiredArgsConstructor
public class SettlementController {

    private final PlatformRepository platformRepository;
    private final PlatformSettlementRepository settlementRepository;

    // ── 플랫폼 목록 조회 ──
    @GetMapping("/platforms")
    public ResponseEntity<?> getPlatforms() {
        var list = platformRepository.findByActiveTrueOrderByNameAsc();
        var result = list.stream().map(p -> Map.of(
                "id",   p.getId(),
                "name", p.getName()
        )).toList();
        return ResponseEntity.ok(result);
    }

    // ── 플랫폼 추가 ──
    @PostMapping("/platforms")
    public ResponseEntity<?> addPlatform(@RequestBody Map<String, String> body) {
        try {
            String name = body.get("name");
            if (name == null || name.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "플랫폼 이름을 입력해주세요."));
            Platform p = new Platform();
            setField(p, "name", name.trim());
            setField(p, "active", true);
            platformRepository.save(p);
            return ResponseEntity.ok(Map.of("message", "플랫폼이 추가되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage()));
        }
    }

    // ── 플랫폼 삭제 (비활성화) ──
    @DeleteMapping("/platforms/{id}")
    public ResponseEntity<?> deletePlatform(@PathVariable Long id) {
        try {
            Platform p = platformRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("플랫폼을 찾을 수 없습니다."));
            setField(p, "active", false);
            platformRepository.save(p);
            return ResponseEntity.ok(Map.of("message", "플랫폼이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── 월별 정산 조회 ──
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthly(@RequestParam Integer year, @RequestParam Integer month) {
        var list = settlementRepository.findByYearAndMonthOrderByPlatformNameAsc(year, month);
        var result = list.stream().map(s -> Map.of(
                "id",           s.getId(),
                "platformId",   s.getPlatform().getId(),
                "platformName", s.getPlatform().getName(),
                "year",         s.getYear(),
                "month",        s.getMonth(),
                "amount",       s.getAmount(),
                "region",       s.getRegion(),
                "memo",         s.getMemo() != null ? s.getMemo() : ""
        )).toList();
        return ResponseEntity.ok(result);
    }

    // ── 정산 저장 (없으면 생성, 있으면 수정) ──
    @PostMapping("/save")
    public ResponseEntity<?> saveSettlement(@RequestBody Map<String, Object> body) {
        try {
            Long platformId = Long.valueOf(body.get("platformId").toString());
            Integer year    = (Integer) body.get("year");
            Integer month   = (Integer) body.get("month");
            Long amount     = Long.valueOf(body.get("amount").toString());
            String region   = (String) body.getOrDefault("region", "국내");
            String memo     = (String) body.getOrDefault("memo", "");

            Platform platform = platformRepository.findById(platformId)
                    .orElseThrow(() -> new IllegalArgumentException("플랫폼을 찾을 수 없습니다."));

            PlatformSettlement settlement = settlementRepository
                    .findByPlatformIdAndYearAndMonthAndRegion(platformId, year, month, region)
                    .orElse(new PlatformSettlement());

            setField(settlement, "platform", platform);
            setField(settlement, "year",     year);
            setField(settlement, "month",    month);
            setField(settlement, "amount",   amount);
            setField(settlement, "region",   region);
            setField(settlement, "memo",     memo);
            settlementRepository.save(settlement);

            return ResponseEntity.ok(Map.of("message", "저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "저장 실패: " + e.getMessage()));
        }
    }

    // ── 정산 삭제 ──
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSettlement(@PathVariable Long id) {
        settlementRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 연간 합계 조회 ──
    @GetMapping("/yearly")
    public ResponseEntity<?> getYearly(@RequestParam Integer year) {
        var list = settlementRepository.findByYearOrderByMonthAscPlatformNameAsc(year);
        var result = list.stream().map(s -> Map.of(
                "platformName", s.getPlatform().getName(),
                "month",        s.getMonth(),
                "amount",       s.getAmount(),
                "region",       s.getRegion()
        )).toList();
        return ResponseEntity.ok(result);
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
