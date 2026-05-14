package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.User;
import com.example.seoulcitytour.repository.UserRepository;
import com.example.seoulcitytour.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository     userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder    passwordEncoder;

    // active=true 계정만 조회 (ADMIN, DEV 제외)
    public List<User> getManagedUsers() {
        return userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> !u.getRole().equals("ROLE_ADMIN") && !u.getRole().equals("ROLE_DEV"))
                .toList();
    }

    // 계정 생성
    public User createUser(String username, String password, String role, String name) {
        // ADMIN, DEV 는 생성 불가
        if ("ROLE_ADMIN".equals(role) || "ROLE_DEV".equals(role))
            throw new IllegalArgumentException("생성할 수 없는 역할입니다: " + role);

        // 역할이 user_role 테이블에 존재하는지 확인
        if (!userRoleRepository.existsByRoleKey(role))
            throw new IllegalArgumentException("존재하지 않는 역할입니다: " + role);

        // 같은 username 중복 체크
        userRepository.findByUsername(username).ifPresent(u -> {
            if (u.getActive()) throw new IllegalArgumentException("이미 존재하는 아이디입니다: " + username);
        });

        User user = new User();
        try {
            setField(user, "username", username);
            setField(user, "password", passwordEncoder.encode(password));
            setField(user, "role",     role);
            setField(user, "name",     name);
            setField(user, "active",   true);
        } catch (Exception e) { throw new RuntimeException("계정 생성 실패", e); }
        return userRepository.save(user);
    }

    // 소프트 삭제
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        if ("ROLE_ADMIN".equals(user.getRole()) || "ROLE_DEV".equals(user.getRole()))
            throw new IllegalArgumentException("삭제할 수 없는 계정입니다.");

        try { setField(user, "active", false); }
        catch (Exception e) { throw new RuntimeException("삭제 실패", e); }
        userRepository.save(user);
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}