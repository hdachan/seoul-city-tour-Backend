package com.example.seoulcitytour.scheduler;

import com.example.seoulcitytour.entity.SalesMonthLock;
import com.example.seoulcitytour.repository.SalesMonthLockRepository;
import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesWeekLockScheduler {

    private final SalesMonthLockRepository lockRepository;
    private final UserRepository           userRepository;

    // 서버 시작 시 한 번 실행
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("서버 시작 - 주 잠금 상태 초기화");
        autoLockPastWeeks();
    }

    // 매일 자정 실행 - 이번 주 오픈 + 지난 주 자동 잠금
    @Scheduled(cron = "0 0 0 * * *")
    public void autoLockPastWeeks() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        int year  = today.getYear();
        int month = today.getMonthValue();

        var salesUsers = userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> "ROLE_SALES".equals(u.getRole()))
                .toList();

        List<WeekRange> weeks = getWeeksOfMonth(year, month);
        int currentWeekNum = getCurrentWeekNum(year, month);

        for (var user : salesUsers) {
            for (var week : weeks) {
                boolean isPast    = week.end().isBefore(today);
                boolean isCurrent = week.weekNum() == currentWeekNum;

                try {
                    var existing = lockRepository.findBySalesUsernameAndYearAndMonthAndWeekNum(
                            user.getUsername(), year, month, week.weekNum());
                    SalesMonthLock lock = existing.orElse(new SalesMonthLock());

                    if (isPast) {
                        // 지난 주 → 잠금
                        if (existing.isEmpty() || !existing.get().getLocked()) {
                            setField(lock, "salesUsername", user.getUsername());
                            setField(lock, "year",    year);
                            setField(lock, "month",   month);
                            setField(lock, "weekNum", week.weekNum());
                            setField(lock, "locked",  true);
                            lockRepository.save(lock);
                            log.info("자동 잠금: {} {}월 {}주", user.getUsername(), month, week.weekNum());
                        }
                    } else if (isCurrent) {
                        // 이번 주 → 명시적으로 오픈 (관리자가 잠근 경우 유지)
                        if (existing.isEmpty()) {
                            setField(lock, "salesUsername", user.getUsername());
                            setField(lock, "year",    year);
                            setField(lock, "month",   month);
                            setField(lock, "weekNum", week.weekNum());
                            setField(lock, "locked",  false);
                            lockRepository.save(lock);
                            log.info("자동 오픈: {} {}월 {}주", user.getUsername(), month, week.weekNum());
                        }
                    }
                } catch (Exception e) {
                    log.error("주 잠금 처리 실패: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 월요일 기준 주 계산
     * 한국 달력 기준 (월~일)
     */
    public static List<WeekRange> getWeeksOfMonth(int year, int month) {
        List<WeekRange> weeks = new ArrayList<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay  = firstDay.with(TemporalAdjusters.lastDayOfMonth());

        // 첫 번째 월요일 찾기 (1일이 월요일이 아니면 이전 달 월요일부터)
        LocalDate start = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        int weekNum = 1;
        while (!start.isAfter(lastDay)) {
            LocalDate end = start.plusDays(6); // 일요일

            // 이번 달 범위로 클램핑
            LocalDate clampedStart = start.isBefore(firstDay) ? firstDay : start;
            LocalDate clampedEnd   = end.isAfter(lastDay)     ? lastDay  : end;

            weeks.add(new WeekRange(weekNum, clampedStart, clampedEnd));
            start = start.plusWeeks(1);
            weekNum++;
        }
        return weeks;
    }

    /**
     * 오늘이 속한 주 번호 반환
     */
    public static int getCurrentWeekNum(int year, int month) {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        for (var week : getWeeksOfMonth(year, month)) {
            if (!today.isBefore(week.start()) && !today.isAfter(week.end())) {
                return week.weekNum();
            }
        }
        return -1;
    }

    public record WeekRange(int weekNum, LocalDate start, LocalDate end) {
        public String label() {
            return start.getDayOfMonth() + "일~" + end.getDayOfMonth() + "일";
        }
    }

    private void setField(Object obj, String fn, Object val) throws Exception {
        Field f = obj.getClass().getDeclaredField(fn);
        f.setAccessible(true);
        f.set(obj, val);
    }
}