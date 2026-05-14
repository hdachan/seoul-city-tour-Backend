package com.example.seoulcitytour.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // @PreAuthorize 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter       jwtFilter;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 로그인은 누구나
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // 역할/탭 조회는 ADMIN, DEV 모두 허용
                        .requestMatchers(HttpMethod.GET, "/api/dev/roles").hasAnyRole("ADMIN", "DEV")
                        .requestMatchers(HttpMethod.GET, "/api/dev/tabs").hasAnyRole("ADMIN", "DEV")
                        .requestMatchers(HttpMethod.GET, "/api/dev/permissions/**").authenticated()

                        // 역할 추가/수정/삭제는 DEV만
                        .requestMatchers(HttpMethod.POST,   "/api/dev/roles").hasRole("DEV")
                        .requestMatchers(HttpMethod.PUT,    "/api/dev/roles/**").hasRole("DEV")
                        .requestMatchers(HttpMethod.DELETE, "/api/dev/roles/**").hasRole("DEV")

                        // 계정 관리는 @PreAuthorize에서 탭 권한으로 제어
                        // .requestMatchers("/api/admin/**").hasRole("ADMIN") ← 제거

                        // 나머지는 로그인만 되면 접근 허용
                        // (각 컨트롤러의 @PreAuthorize에서 탭 권한으로 세부 제어)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter,       UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowCredentials(true);
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}