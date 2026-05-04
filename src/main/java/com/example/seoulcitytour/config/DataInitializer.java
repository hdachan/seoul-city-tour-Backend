package com.example.seoulcitytour.config;

import com.example.seoulcitytour.entity.User;
import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) return;

        createUser("admin", "1234", "ROLE_ADMIN", "관리자");
        createUser("sales", "1234", "ROLE_SALES", "영업담당자");
        createUser("guide", "1234", "ROLE_GUIDE", "가이드");
        createUser("dev",   "1234", "ROLE_DEV",   "개발자");

        System.out.println("✅ 테스트 계정 4개 생성 완료!");
    }

    private void createUser(String username, String password, String role, String name) {
        try {
            User user = new User();
            setField(user, "username", username);
            setField(user, "password", passwordEncoder.encode(password));
            setField(user, "role",     role);
            setField(user, "name",     name);
            setField(user, "active",   true);
            userRepository.save(user);
        } catch (Exception e) { throw new RuntimeException("계정 생성 실패", e); }
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
