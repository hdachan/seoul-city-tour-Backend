package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/dashboard")
    public Map<String, String> dashboard() {
        return Map.of("message", "[ADMIN] 관리자 대시보드");
    }

    // 계정 목록 (name 포함)
    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        var result = userService.getManagedUsers().stream().map(u -> Map.of(
                "id",       u.getId(),
                "username", u.getUsername(),
                "name",     u.getName() != null ? u.getName() : "",
                "role",     u.getRole()
        )).toList();
        return ResponseEntity.ok(result);
    }

    // 계정 생성 (name 포함)
    @PostMapping("/users/create")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String role     = body.get("role");
            String name     = body.get("name");

            if (username == null || password == null || role == null || name == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "모든 항목을 입력해주세요."));
            }

            userService.createUser(username, password, role, name);
            return ResponseEntity.ok(Map.of("message", "계정이 생성되었습니다."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 계정 삭제
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "계정이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
