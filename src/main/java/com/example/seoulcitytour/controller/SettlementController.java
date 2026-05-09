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

    private final PlatformRepository           platformRepository;
    private final PlatformSettlementRepository settlementRepository;

    // ── 플랫폼 목록 (active만) ──
    @GetMapping("/platforms")
    public ResponseEntity<?> getPlatforms() {
        var list = platformRepository.findAll().stream()
                .filter(p -> p.getActive())
                .map(p -> Map.of(
                        "id",     p.getId(),
                        "name",   p.getName(),
                        "region", p.getRegion() != null ? p.getRegion() : "",
                        "memo",   p.getMemo()   != null ? p.getMemo()   : ""
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    // ── 플랫폼 추가 (이름 + 지역 + 비고 설정) ──
    @PostMapping("/platforms")
    public ResponseEntity<?> addPlatform(@RequestBody Map<String, Object> body) {
        try {
            Platform p = new Platform();
            setField(p, "name",   ((String) body.get("name")).trim());
            setField(p, "region", body.getOrDefault("region", "국내"));
            setField(p, "memo",   body.getOrDefault("memo", ""));
            setField(p, "active", true);
            platformRepository.save(p);
            return ResponseEntity.ok(Map.of("message", "추가되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage()));
        }
    }

    // ── 플랫폼 수정 (이름/지역/비고 변경) ──
    @PutMapping("/platforms/{id}")
    public ResponseEntity<?> updatePlatform(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        try {
            Platform p = platformRepository.findById(id).orElseThrow();
            setField(p, "name",   ((String) body.get("name")).trim());
            setField(p, "region", body.getOrDefault("region", "국내"));
            setField(p, "memo",   body.getOrDefault("memo", ""));
            platformRepository.save(p);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage()));
        }
    }

    // ── 플랫폼 삭제 (소프트) ──
    @DeleteMapping("/platforms/{id}")
    public ResponseEntity<?> deletePlatform(@PathVariable Long id) {
        try {
            Platform p = platformRepository.findById(id).orElseThrow();
            setField(p, "active", false);
            platformRepository.save(p);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── 월별 정산 조회 (플랫폼별 금액, 없으면 빈값) ──
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthly(@RequestParam Integer year,
                                        @RequestParam Integer month) {
        var platforms = platformRepository.findAll().stream()
                .filter(p -> p.getActive()).toList();

        var result = platforms.stream().map(p -> {
            var settlement = settlementRepository
                    .findByPlatformIdAndYearAndMonth(p.getId(), year, month)
                    .orElse(null);
            return Map.of(
                    "platformId", p.getId(),
                    "name",       p.getName(),
                    "region",     p.getRegion() != null ? p.getRegion() : "국내",
                    "memo",       settlement != null && settlement.getMemo() != null
                                    ? settlement.getMemo()
                                    : (p.getMemo() != null ? p.getMemo() : ""),
                    "amount",     settlement != null && settlement.getAmount() != null
                                    ? settlement.getAmount() : 0L,
                    "hasData",    settlement != null
            );
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ── 정산 저장 (upsert - 있으면 수정, 없으면 추가) ──
    @PostMapping("/save")
    public ResponseEntity<?> saveSettlement(@RequestBody Map<String, Object> body) {
        try {
            Long    platformId = Long.parseLong(body.get("platformId").toString());
            Integer year       = (Integer) body.get("year");
            Integer month      = (Integer) body.get("month");
            Long    amount     = body.get("amount") != null && !body.get("amount").toString().isBlank()
                                    ? Long.parseLong(body.get("amount").toString()) : 0L;
            String  memo       = (String) body.getOrDefault("memo", "");

            // 이미 있으면 업데이트, 없으면 새로 생성
            PlatformSettlement ps = settlementRepository
                    .findByPlatformIdAndYearAndMonth(platformId, year, month)
                    .orElse(new PlatformSettlement());

            Platform platform = platformRepository.findById(platformId).orElseThrow();
            setField(ps, "platform", platform);
            setField(ps, "year",     year);
            setField(ps, "month",    month);
            setField(ps, "amount",   amount);
            setField(ps, "memo",     memo);
            settlementRepository.save(ps);
            return ResponseEntity.ok(Map.of("message", "저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "저장 실패: " + e.getMessage()));
        }
    }

    // ── 연간 정산 ──
    @GetMapping("/yearly")
    public ResponseEntity<?> getYearly(@RequestParam Integer year) {
        var list = settlementRepository.findAll().stream()
                .filter(s -> s.getYear().equals(year))
                .map(s -> Map.of(
                        "month",  s.getMonth(),
                        "name",   s.getPlatform().getName(),
                        "region", s.getPlatform().getRegion() != null ? s.getPlatform().getRegion() : "",
                        "amount", s.getAmount() != null ? s.getAmount() : 0L,
                        "memo",   s.getMemo() != null ? s.getMemo() : ""
                )).toList();
        return ResponseEntity.ok(list);
    }

    private void setField(Object obj, String fn, Object val) throws Exception {
        Field f = obj.getClass().getDeclaredField(fn);
        f.setAccessible(true);
        f.set(obj, val);
    }
}
