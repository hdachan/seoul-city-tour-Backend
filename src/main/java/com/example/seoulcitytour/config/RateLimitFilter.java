package com.example.seoulcitytour.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // IP별 시도 횟수 + 마지막 시도 시간
    private final Map<String, AtomicInteger> attempts  = new ConcurrentHashMap<>();
    private final Map<String, Long>          blockUntil = new ConcurrentHashMap<>();

    private static final int    MAX_ATTEMPTS   = 5;        // 최대 5회
    private static final long   BLOCK_DURATION = 60_000L;  // 1분 차단
    private static final long   WINDOW         = 300_000L; // 5분 안에 5회 초과 시 차단

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // 로그인 엔드포인트만 제한
        if (!"/api/auth/login".equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        long now  = System.currentTimeMillis();

        // 차단 중인지 확인
        Long blockedUntil = blockUntil.get(ip);
        if (blockedUntil != null && now < blockedUntil) {
            long remain = (blockedUntil - now) / 1000;
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"error\": \"너무 많은 로그인 시도입니다. " + remain + "초 후 다시 시도해주세요.\"}"
            );
            return;
        }

        // 차단 해제 후 카운터 리셋
        if (blockedUntil != null && now >= blockedUntil) {
            blockUntil.remove(ip);
            attempts.remove(ip);
        }

        // 시도 횟수 증가
        AtomicInteger count = attempts.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int current = count.incrementAndGet();

        if (current > MAX_ATTEMPTS) {
            blockUntil.put(ip, now + BLOCK_DURATION);
            attempts.remove(ip);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"error\": \"로그인 시도가 너무 많습니다. 1분 후 다시 시도해주세요.\"}"
            );
            return;
        }

        chain.doFilter(request, response);

        // 로그인 성공 시 카운터 리셋
        if (response.getStatus() == 200) {
            attempts.remove(ip);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip;
        return request.getRemoteAddr();
    }
}
