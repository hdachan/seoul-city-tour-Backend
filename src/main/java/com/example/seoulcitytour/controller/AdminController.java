package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.User;
import com.example.seoulcitytour.repository.UserRepository;
import com.example.seoulcitytour.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository  userRepository;
    private final UserService     userService;

    // 요청한 사람의 역할 (토큰 대신 DB 기준)
    private String roleOf(Authentication auth) {
        return userRepository.findByUsername(auth.getName()).map(User::getRole).orElse("");
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(Authentication auth) {
        return ResponseEntity.ok(userService.getUsersFor(roleOf(auth)).stream().map(u -> Map.of(
                "id",       u.getId(),
                "username", u.getUsername(),
                "name",     u.getName() != null ? u.getName() : "",
                "role",     u.getRole(),
                "me",       u.getUsername().equals(auth.getName())
        )).toList());
    }

    @PostMapping("/users/create")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            userService.createUser(
                    (String) body.get("username"),
                    (String) body.get("password"),
                    (String) body.get("role"),
                    (String) body.getOrDefault("name", ""),
                    roleOf(auth)
            );
            return ResponseEntity.ok(Map.of("message", "계정이 생성되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                        Authentication auth) {
        try {
            userService.updateUser(id,
                    (String) body.get("name"),
                    (String) body.get("role"),
                    (String) body.get("newPassword"),
                    auth.getName(), roleOf(auth));
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication auth) {
        try {
            userService.deleteUser(id, auth.getName(), roleOf(auth));
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
