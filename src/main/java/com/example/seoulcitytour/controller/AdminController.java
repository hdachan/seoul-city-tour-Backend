package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.User;
import com.example.seoulcitytour.repository.UserRepository;
import com.example.seoulcitytour.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository  userRepository;
    private final UserService     userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(userService.getManagedUsers().stream().map(u -> Map.of(
                "id",       u.getId(),
                "username", u.getUsername(),
                "name",     u.getName() != null ? u.getName() : "",
                "role",     u.getRole()
        )).toList());
    }

    @PostMapping("/users/create")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> body) {
        try {
            userService.createUser(
                    (String) body.get("username"),
                    (String) body.get("password"),
                    (String) body.get("role"),
                    (String) body.getOrDefault("name", "")
            );
            return ResponseEntity.ok(Map.of("message", "계정이 생성되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            User user = userRepository.findById(id).orElseThrow();
            String name        = (String) body.get("name");
            String role        = (String) body.get("role");
            String newPassword = (String) body.get("newPassword");
            if (name        != null) setField(user, "name", name);
            if (role        != null) setField(user, "role", role);
            if (newPassword != null && !newPassword.isBlank())
                setField(user, "password", passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private void setField(Object obj, String fn, Object val) throws Exception {
        Field f = obj.getClass().getDeclaredField(fn);
        f.setAccessible(true);
        f.set(obj, val);
    }
}