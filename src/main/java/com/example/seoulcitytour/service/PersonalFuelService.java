package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.SalesDriving;
import com.example.seoulcitytour.repository.SalesDrivingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 개인주유 잔액(이월) 계산 - DB에 저장하지 않고 운행 기록으로 매번 계산
 *
 *  잔액 = 지난달 잔액 + 이번 달 개인주유한 양(L) - 이번 달 넣어야 할 양(L)
 *  넣어야 할 양 = 개인사용 km ÷ 평균연비
 *  평균연비    = 그 달 주행거리 ÷ 그 달 주유량(주유+개인주유)
 *               (그 달 주유 기록이 없으면 그 달 말까지 전체 기간 평균)
 *  거리 계산은 월간 통계 화면과 동일 (전월 마지막 미터기 기준, 구간은 뒤쪽 기록 구분에 붙음)
 */
@Service
@RequiredArgsConstructor
public class PersonalFuelService {

    private final SalesDrivingRepository drivingRepository;

    /** 이월 시작 달 - null 이면 그 사람의 첫 운행 기록 달부터 */
    private static final YearMonth CARRYOVER_START = null;

    public Map<String, Object> calcBalance(String username, int year, int month) {
        YearMonth target = YearMonth.of(year, month);
        YearMonth start = CARRYOVER_START;
        if (start == null) {
            SalesDriving first = drivingRepository.findFirstBySalesUsernameOrderByDateAsc(username);
            start = first != null ? YearMonth.from(first.getDate()) : target;
        }

        List<Map<String, Object>> months = new ArrayList<>();
        double balance = 0;
        double carriedIn = 0;
        Map<String, Object> current = null;

        for (YearMonth ym = start; !ym.isAfter(target); ym = ym.plusMonths(1)) {
            Map<String, Object> m = calcMonth(username, ym);
            double before = balance;
            double putL = (double) m.get("putL");
            double needL = (double) m.get("needL");
            balance = balance + putL - needL;
            m.put("putL", round1(putL));
            m.put("needL", round1(needL));
            m.put("carriedIn", round1(before));
            m.put("balance", round1(balance));
            months.add(m);
            if (ym.equals(target)) {
                carriedIn = before;
                current = m;
            }
        }

        var res = new LinkedHashMap<String, Object>();
        res.put("startMonth", start.toString());
        res.put("carriedIn", round1(carriedIn));            // 지난달에서 넘어온 양
        res.put("balance", round1(balance));                // 다음 달로 넘어가는 양 (음수면 부족)
        res.put("current", current);                         // 이번 달 상세 (시작 달 이전이면 null)
        res.put("months", months);                           // 달별 내역
        return res;
    }

    private Map<String, Object> calcMonth(String username, YearMonth ym) {
        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        var list = drivingRepository.findBySalesUsernameAndYearAndMonthOrderByDateAscIdAsc(
                username, ym.getYear(), ym.getMonthValue());

        // 전월 마지막 미터기
        var prevList = drivingRepository.findPrevMeterReadings(username, firstDay);
        int last = prevList.isEmpty() || prevList.get(0).getMeterReading() == null
                ? 0 : prevList.get(0).getMeterReading();

        // 거리: 날짜 → 미터기 순, 구간은 뒤쪽 기록 구분에 붙음
        var sorted = list.stream()
                .filter(d -> d.getMeterReading() != null && d.getMeterReading() > 0)
                .sorted(Comparator.comparing(SalesDriving::getDate)
                        .thenComparing(SalesDriving::getMeterReading))
                .toList();
        long workKm = 0, personalKm = 0;
        for (var d : sorted) {
            int meter = d.getMeterReading();
            if (last > 0 && meter > last) {
                if ("개인사용".equals(d.getType())) personalKm += meter - last;
                else workKm += meter - last;
            }
            last = Math.max(last, meter);
        }

        double monthFuelL = 0, putL = 0;
        for (var d : list) {
            double l = d.getFuelAmount() != null ? d.getFuelAmount() : 0;
            if ("주유".equals(d.getType()) || "개인주유".equals(d.getType())) monthFuelL += l;
            if ("개인주유".equals(d.getType())) putL += l;
        }

        // 평균연비
        long monthDist = workKm + personalKm;
        Double kmPerL = null;
        String basis = "";
        if (monthFuelL > 0 && monthDist > 0) {
            kmPerL = monthDist / monthFuelL;
            basis = "month";
        } else {
            Integer min = drivingRepository.findMinMeterUntil(username, lastDay);
            Integer max = drivingRepository.findMaxMeterUntil(username, lastDay);
            Double totalL = drivingRepository.sumFuelAmountUntil(username, lastDay);
            if (min != null && max != null && max > min && totalL != null && totalL > 0) {
                kmPerL = (max - min) / totalL;
                basis = "overall";
            }
        }
        // 개인사용은 있는데 연비를 낼 주유 기록이 전혀 없으면 계산 불가
        // → 잔액에서 빼지 않고 needUnknown 으로 표시 (화면에서 안내)
        boolean needUnknown = personalKm > 0 && (kmPerL == null || kmPerL <= 0);
        double needL = (!needUnknown && kmPerL != null) ? personalKm / kmPerL : 0;

        var m = new LinkedHashMap<String, Object>();
        m.put("month", ym.toString());
        m.put("workKm", workKm);
        m.put("personalKm", personalKm);
        m.put("kmPerL", kmPerL != null ? round1(kmPerL) : null);
        m.put("basis", basis);
        m.put("needL", needL);
        m.put("needUnknown", needUnknown);
        m.put("putL", putL);
        return m;
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
