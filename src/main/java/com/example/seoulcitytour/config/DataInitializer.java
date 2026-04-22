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
        // 이미 데이터 있으면 추가 안 함
        if (userRepository.count() > 0) return;

        // 테스트 계정 4개 자동 생성
        createUser("admin", "1234", "ROLE_ADMIN");
        createUser("sales", "1234", "ROLE_SALES");
        createUser("guide", "1234", "ROLE_GUIDE");
        createUser("dev",   "1234", "ROLE_DEV");

        System.out.println("✅ 테스트 계정 4개 생성 완료!");
    }

    private void createUser(String username, String password, String role) {
        // User 엔티티는 Lombok @NoArgsConstructor + 직접 INSERT
        userRepository.save(createUserEntity(username, password, role));
    }

    private User createUserEntity(String username, String rawPassword, String role) {
        try {
            User user = new User();
            // Reflection으로 필드 설정 (setter 없는 경우)
            setField(user, "username", username);
            setField(user, "password", passwordEncoder.encode(rawPassword));
            setField(user, "role", role);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("유저 생성 실패", e);
        }
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
