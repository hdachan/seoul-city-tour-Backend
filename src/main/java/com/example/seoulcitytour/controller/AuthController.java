package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.config.InputValidator;
import com.example.seoulcitytour.config.JwtUtil;
import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil               jwtUtil;
    private final UserRepository        userRepository;
    private final InputValidator        validator;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 입력값 기본 검증
        if (username == null || username.isBlank() || password == null || password.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "아이디와 비밀번호를 입력해주세요."));

        // SQL Injection 방어
        try {
            validator.validateSafe("username", username);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 입력값입니다."));
        }

        // 아이디 형식 검사 (영문+숫자, 4~20자)
        if (!validator.isValidUsername(username))
            return ResponseEntity.badRequest().body(Map.of("error", "아이디 형식이 올바르지 않습니다."));

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "아이디 또는 비밀번호가 틀렸습니다."));
        }

        var user  = userRepository.findByUsername(username).orElseThrow();
        String token = jwtUtil.generateToken(username, user.getRole());

        return ResponseEntity.ok(Map.of(
                "token",    token,
                "username", username,
                "role",     user.getRole(),
                "name",     user.getName() != null ? user.getName() : username
        ));
    }
}
