package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.User;
import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;

/**
 * 영업 법인카드 번호
 *  - 카드번호가 없으면 영업 정산 화면에서 먼저 입력 (시작 미터기와 같은 방식)
 *  - 본인은 처음 한 번만, 이후 수정은 관리자
 */
@Service
@RequiredArgsConstructor
public class SalesCardService {

    private final UserRepository userRepository;

    /** 카드번호 저장 - 문제 있으면 안내 문구, 없으면 null */
    public String save(String username, Object value, boolean onlyFirst) throws Exception {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return "사용자를 찾을 수 없습니다.";
        if (onlyFirst && user.getCardNumber() != null && !user.getCardNumber().isBlank())
            return "이미 등록했습니다. 수정은 관리자에게 요청해주세요.";

        String card = value == null ? "" : String.valueOf(value).trim();
        if (card.isEmpty()) return "카드번호를 입력해주세요.";
        if (!card.matches("[0-9\\- ]+")) return "카드번호는 숫자와 - 만 입력할 수 있습니다.";

        Field f = User.class.getDeclaredField("cardNumber");
        f.setAccessible(true);
        f.set(user, card);
        userRepository.save(user);
        return null;
    }
}
