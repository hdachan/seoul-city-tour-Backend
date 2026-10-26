package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.User;
import com.example.seoulcitytour.repository.SalesDrivingRepository;
import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 영업 시작 미터기
 *  - 처음 쓰는 사람은 운행 기록이 없어서 첫 운행거리를 계산할 기준이 없음
 *  - 사람마다 시작 미터기를 하나 저장해 두고, 이전 기록이 없을 때 기준으로 사용
 *  - 미터기 기록이 하나도 없고 시작 미터기도 없으면 → 영업 정산 화면에서 먼저 입력 (needed)
 */
@Service
@RequiredArgsConstructor
public class StartMeterService {

    private final UserRepository userRepository;
    private final SalesDrivingRepository drivingRepository;

    /** 시작 미터기 (없으면 0) */
    public int get(String username) {
        return userRepository.findByUsername(username)
                .map(User::getStartMeter)
                .filter(m -> m != null && m > 0)
                .orElse(0);
    }

    /** date 이전 마지막 미터기 - 이전 기록이 없으면 시작 미터기 (미터기 0 인 기록은 없는 것으로 봄) */
    public int prevMeter(String username, LocalDate date) {
        return drivingRepository.findPrevMeterReadings(username, date).stream()
                .map(d -> d.getMeterReading())
                .filter(m -> m != null && m > 0)
                .findFirst()
                .orElseGet(() -> get(username));
    }

    /** 화면용: { startMeter, needed } */
    public Map<String, Object> status(String username) {
        int start = get(username);
        var res = new LinkedHashMap<String, Object>();
        res.put("startMeter", start > 0 ? start : null);
        res.put("needed", start == 0 && drivingRepository.findMinMeter(username) == null);
        return res;
    }

    /**
     * 시작 미터기 저장 - 문제 있으면 안내 문구, 없으면 null
     * onlyFirst: 본인은 처음 한 번만 (이후 수정은 관리자)
     */
    public String save(String username, Object value, boolean onlyFirst) throws Exception {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "사용자를 찾을 수 없습니다.";
        if (onlyFirst && user.getStartMeter() != null && user.getStartMeter() > 0)
            return "이미 입력했습니다. 수정은 관리자에게 요청해주세요.";

        int meter;
        try {
            meter = Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return "미터기 숫자를 입력해주세요.";
        }
        if (meter <= 0) return "미터기 숫자를 입력해주세요.";

        Integer firstMeter = drivingRepository.findMinMeter(username);
        if (firstMeter != null && meter > firstMeter)
            return "첫 운행 기록 미터기(" + firstMeter + "km)보다 클 수 없습니다. 입력값: " + meter + "km";

        Field f = User.class.getDeclaredField("startMeter");
        f.setAccessible(true);
        f.set(user, meter);
        userRepository.save(user);
        return null;
    }
}
