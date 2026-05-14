package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.config.JwtUtil;
import com.example.seoulcitytour.config.InputValidator;
import com.example.seoulcitytour.entity.TabPermission;
import com.example.seoulcitytour.repository.TabPermissionRepository;
import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager   authenticationManager;
    private final JwtUtil                 jwtUtil;
    private final UserRepository          userRepository;
    private final TabPermissionRepository tabPermissionRepository;
    private final InputValidator          validator;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String rawUsername = body.get("username");
        String password    = body.get("password");

        if (rawUsername == null || rawUsername.isBlank() || password == null || password.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "아이디와 비밀번호를 입력해주세요."));

        // 대소문자 무관하게 항상 소문자로 정규화
        String username = rawUsername.trim().toLowerCase();

        try { validator.validateSafe("username", username); }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 입력값입니다."));
        }

        if (!validator.isValidUsername(username))
            return ResponseEntity.badRequest().body(Map.of("error", "아이디 형식이 올바르지 않습니다."));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "아이디 또는 비밀번호가 틀렸습니다."));
        }

        var user = userRepository.findByUsername(username).orElseThrow();
        String token = jwtUtil.generateToken(username, user.getRole());

        // 해당 역할의 허용 탭 목록
        List<String> allowedTabs = tabPermissionRepository.findByRoleKey(user.getRole())
                .stream().map(TabPermission::getTabId).toList();

        return ResponseEntity.ok(Map.of(
                "token",       token,
                "username",    username,          // 항상 소문자로 반환
                "role",        user.getRole(),
                "name",        user.getName() != null ? user.getName() : username,
                "allowedTabs", allowedTabs
        ));
    }
}