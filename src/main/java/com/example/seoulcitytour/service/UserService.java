package com.example.seoulcitytour.service;

import com.example.seoulcitytour.entity.User;
import com.example.seoulcitytour.repository.UserRepository;
import com.example.seoulcitytour.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 계정 관리
 *  - 관리자: 영업·가이드 등 일반 계정만 보고 만들고 고침
 *  - 개발자: 관리자·개발자 계정까지 전부 (예: 사장님 관리자 계정 만들기)
 *  - 누구나: 내 비밀번호 변경
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ADMIN = "ROLE_ADMIN";
    private static final String DEV   = "ROLE_DEV";
    private static final int    MIN_PASSWORD = 4;   // 로그인 화면 검사와 같음
    private static final int    MAX_PASSWORD = 50;

    private final UserRepository     userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder    passwordEncoder;

    private static boolean isPrivileged(String role) { return ADMIN.equals(role) || DEV.equals(role); }

    // 화면에 보일 계정 (active=true) - 개발자는 전부, 그 외는 관리자·개발자 제외
    public List<User> getUsersFor(String callerRole) {
        return userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> DEV.equals(callerRole) || !isPrivileged(u.getRole()))
                .toList();
    }

    // 계정 생성 - 관리자·개발자 계정은 개발자만
    public User createUser(String username, String password, String role, String name, String callerRole) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("아이디를 입력해주세요.");
        final String normalizedUsername = username.trim().toLowerCase();

        if (isPrivileged(role) && !DEV.equals(callerRole))
            throw new IllegalArgumentException("관리자·개발자 계정은 개발자만 만들 수 있습니다.");

        // 역할이 user_role 테이블에 존재하는지 확인
        if (!userRoleRepository.existsByRoleKey(role))
            throw new IllegalArgumentException("존재하지 않는 역할입니다: " + role);

        checkPassword(password);

        // 같은 username 중복 체크
        userRepository.findByUsername(normalizedUsername).ifPresent(u -> {
            if (u.getActive()) throw new IllegalArgumentException("이미 존재하는 아이디입니다: " + normalizedUsername);
        });

        User user = new User();
        try {
            setField(user, "username", normalizedUsername);
            setField(user, "password", passwordEncoder.encode(password));
            setField(user, "role",     role);
            setField(user, "name",     name);
            setField(user, "active",   true);
        } catch (Exception e) { throw new RuntimeException("계정 생성 실패", e); }
        return userRepository.save(user);
    }

    // 계정 수정 (이름 · 역할 · 비밀번호 초기화)
    public void updateUser(Long id, String name, String role, String newPassword,
                           String callerUsername, String callerRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        boolean dev = DEV.equals(callerRole);

        if (isPrivileged(user.getRole()) && !dev)
            throw new IllegalArgumentException("관리자·개발자 계정은 개발자만 수정할 수 있습니다.");

        if (role != null && !role.equals(user.getRole())) {
            if (isPrivileged(role) && !dev)
                throw new IllegalArgumentException("관리자·개발자 역할은 개발자만 줄 수 있습니다.");
            if (!userRoleRepository.existsByRoleKey(role))
                throw new IllegalArgumentException("존재하지 않는 역할입니다: " + role);
            if (user.getUsername().equals(callerUsername))
                throw new IllegalArgumentException("내 계정의 역할은 바꿀 수 없습니다.");
            checkNotLast(user, "역할을 바꿀");
        }
        if (newPassword != null && !newPassword.isBlank()) checkPassword(newPassword);

        try {
            if (name != null) setField(user, "name", name);
            if (role != null) setField(user, "role", role);
            if (newPassword != null && !newPassword.isBlank())
                setField(user, "password", passwordEncoder.encode(newPassword));
        } catch (Exception e) { throw new RuntimeException("수정 실패", e); }
        userRepository.save(user);
    }

    // 소프트 삭제 - 관리자·개발자 계정은 개발자만, 내 계정 · 마지막 관리자/개발자는 불가
    public void deleteUser(Long id, String callerUsername, String callerRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        if (isPrivileged(user.getRole()) && !DEV.equals(callerRole))
            throw new IllegalArgumentException("삭제할 수 없는 계정입니다.");
        if (user.getUsername().equals(callerUsername))
            throw new IllegalArgumentException("내 계정은 삭제할 수 없습니다.");
        checkNotLast(user, "삭제할");

        try { setField(user, "active", false); }
        catch (Exception e) { throw new RuntimeException("삭제 실패", e); }
        userRepository.save(user);
    }

    // 내 비밀번호 변경 (현재 비밀번호 확인)
    public void changeMyPassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new IllegalArgumentException("현재 비밀번호가 맞지 않습니다.");
        checkPassword(newPassword);
        if (newPassword.equals(currentPassword))
            throw new IllegalArgumentException("지금 비밀번호와 다른 비밀번호를 입력해주세요.");
        try { setField(user, "password", passwordEncoder.encode(newPassword)); }
        catch (Exception e) { throw new RuntimeException("변경 실패", e); }
        userRepository.save(user);
    }

    private void checkPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD)
            throw new IllegalArgumentException("비밀번호는 " + MIN_PASSWORD + "자 이상이어야 합니다.");
        if (password.length() > MAX_PASSWORD)
            throw new IllegalArgumentException("비밀번호는 " + MAX_PASSWORD + "자까지 입력할 수 있습니다.");
    }

    // 마지막 남은 관리자 · 개발자 계정은 지우거나 역할을 바꿀 수 없음 (아무도 관리 못 하게 되는 것 방지)
    private void checkNotLast(User user, String action) {
        if (!isPrivileged(user.getRole())) return;
        long same = userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> user.getRole().equals(u.getRole()))
                .count();
        if (same <= 1) {
            String label = ADMIN.equals(user.getRole()) ? "관리자" : "개발자";
            throw new IllegalArgumentException("마지막 " + label + " 계정은 " + action + " 수 없습니다.");
        }
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
