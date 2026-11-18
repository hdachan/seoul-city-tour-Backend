package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.SalesHoliday;
import com.example.seoulcitytour.repository.SalesHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

// 🏖 영업 휴무 (영업 사원 본인): 쉬는 날 + 사유
// 표시만 하는 용도라 운행 입력을 막지 않고, 잠금과도 상관없음
@RestController
@RequestMapping("/api/sales-holiday")
@RequiredArgsConstructor
public class SalesHolidayController {

    // 휴무 탭 또는 영업 정산 탭 (영업 정산 화면에서 "휴무" 표시용으로 읽음)
    private static final String CAN_READ =
            "@tabPermissionService.hasAccess(authentication, 'sales-holiday')"
            + " or @tabPermissionService.hasAccess(authentication, 'sales')";
    private static final String CAN_WRITE =
            "@tabPermissionService.hasAccess(authentication, 'sales-holiday')";

    private final SalesHolidayRepository holidayRepository;

    static Map<String, Object> row(SalesHoliday h) {
        return Map.of("date", h.getDate().toString(), "reason", h.getReason() == null ? "" : h.getReason());
    }

    // 내 휴무 (한 달)
    @GetMapping
    @PreAuthorize(CAN_READ)
    public ResponseEntity<?> list(@RequestParam Integer year, @RequestParam Integer month, Authentication auth) {
        YearMonth ym = YearMonth.of(year, month);
        return ResponseEntity.ok(holidayRepository
                .findBySalesUsernameAndDateBetweenOrderByDateAsc(auth.getName(), ym.atDay(1), ym.atEndOfMonth())
                .stream().map(SalesHolidayController::row).toList());
    }

    // 휴무 저장 (그날 이미 있으면 사유만 고침)
    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> save(@RequestBody Map<String, Object> body, Authentication auth) {
        LocalDate date;
        try {
            date = LocalDate.parse(String.valueOf(body.get("date")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "날짜가 올바르지 않습니다."));
        }
        String reason = body.get("reason") == null ? "" : String.valueOf(body.get("reason")).trim();
        if (reason.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "휴무 사유를 적어주세요."));
        if (reason.length() > 500)
            return ResponseEntity.badRequest().body(Map.of("error", "사유는 500자까지 적을 수 있습니다."));

        SalesHoliday h = holidayRepository.findBySalesUsernameAndDate(auth.getName(), date)
                .orElseGet(SalesHoliday::new);
        h.setSalesUsername(auth.getName());
        h.setDate(date);
        h.setReason(reason);
        return ResponseEntity.ok(row(holidayRepository.save(h)));
    }

    // 휴무 취소
    @DeleteMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> delete(@RequestParam String date, Authentication auth) {
        LocalDate d;
        try {
            d = LocalDate.parse(date);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "날짜가 올바르지 않습니다."));
        }
        holidayRepository.findBySalesUsernameAndDate(auth.getName(), d).ifPresent(holidayRepository::delete);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }
}
