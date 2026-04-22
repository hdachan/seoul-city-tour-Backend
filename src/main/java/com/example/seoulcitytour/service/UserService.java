package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.User;
import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> MANAGEABLE_ROLES = List.of("ROLE_SALES", "ROLE_GUIDE");

    // 계정 목록 조회
    public List<User> getManagedUsers() {
        return userRepository.findAll().stream()
                .filter(u -> MANAGEABLE_ROLES.contains(u.getRole()))
                .toList();
    }

    // 계정 생성 (name 추가)
    public User createUser(String username, String password, String role, String name) {
        if (!MANAGEABLE_ROLES.contains(role)) {
            throw new IllegalArgumentException("생성할 수 없는 역할입니다: " + role);
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다: " + username);
        }

        User user = new User();
        try {
            setField(user, "username", username);
            setField(user, "password", passwordEncoder.encode(password));
            setField(user, "role", role);
            setField(user, "name", name);
        } catch (Exception e) {
            throw new RuntimeException("계정 생성 실패", e);
        }
        return userRepository.save(user);
    }

    // 계정 삭제
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        if (!MANAGEABLE_ROLES.contains(user.getRole())) {
            throw new IllegalArgumentException("삭제할 수 없는 계정입니다.");
        }
        userRepository.deleteById(id);
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
