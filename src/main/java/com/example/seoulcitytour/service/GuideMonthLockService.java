package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.GuideMonthLock;
import com.example.seoulcitytour.repository.GuideMonthLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * 가이드 정산 월 잠금 (가이드 화면 · 관리자 화면 공용)
 *  - 관리자가 직접 잠그거나 푼 달은 그 값 그대로
 *  - 손댄 적 없는 달은 그 달이 끝나면(다음 달 1일 0시, 한국시간) 자동으로 잠김
 */
@Service
@RequiredArgsConstructor
public class GuideMonthLockService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final GuideMonthLockRepository lockRepository;

    public boolean isLocked(String guideUsername, int year, int month) {
        return lockRepository.findByGuideUsernameAndYearAndMonth(guideUsername, year, month)
                .map(GuideMonthLock::getLocked)
                .orElseGet(() -> YearMonth.of(year, month).isBefore(YearMonth.now(SEOUL)));
    }

    /** date 가 없으면 오늘 기준 */
    public boolean isLocked(String guideUsername, LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now(SEOUL);
        return isLocked(guideUsername, d.getYear(), d.getMonthValue());
    }
}
