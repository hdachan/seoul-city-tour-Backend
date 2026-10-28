package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.User;
import com.example.seoulcitytour.entity.UserRequest;
import com.example.seoulcitytour.repository.UserRepository;
import com.example.seoulcitytour.repository.UserRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 요청사항
 *  - 관리자(요청사항 탭 권한): 영업·가이드에게 보내기 / 목록 / 삭제
 *  - 받는 사람: 내 요청 보기 → 확인 → 수정 완료
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class UserRequestController {

    private static final Set<String> SENDER_ROLES = Set.of("ROLE_ADMIN", "ROLE_DEV");

    private final UserRequestRepository requestRepository;
    private final UserRepository        userRepository;

    // ── 관리자: 받을 수 있는 사람 (관리자·개발자 제외한 사용 중 계정) ──
    @GetMapping("/admin/targets")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'requests')")
    public ResponseEntity<?> getTargets() {
        return ResponseEntity.ok(userRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(u -> !SENDER_ROLES.contains(u.getRole()))
                .map(u -> Map.of(
                        "username", u.getUsername(),
                        "name",     u.getName() != null ? u.getName() : u.getUsername(),
                        "role",     u.getRole()))
                .toList());
    }

    // ── 관리자: 보낸 요청 목록 (최신순) ──
    @GetMapping("/admin")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'requests')")
    public ResponseEntity<?> getAll() {
        Map<String, String> names = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUsername,
                        u -> u.getName() != null ? u.getName() : u.getUsername(), (a, b) -> a));
        return ResponseEntity.ok(requestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> toMap(r, names::get))
                .toList());
    }

    // ── 관리자: 보내기 (여러 명에게 한 번에) ──
    @PostMapping("/admin")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'requests')")
    public ResponseEntity<?> send(@RequestBody Map<String, Object> body, Authentication auth) {
        String content = body.get("content") == null ? "" : String.valueOf(body.get("content")).trim();
        if (content.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "내용을 입력해주세요."));
        if (content.length() > 1000) return ResponseEntity.badRequest().body(Map.of("error", "내용은 1000자까지 입력할 수 있습니다."));

        List<String> to = body.get("toUsernames") instanceof List<?> l
                ? l.stream().map(String::valueOf).distinct().toList()
                : List.of();
        if (to.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "받는 사람을 선택해주세요."));

        try {
            for (String username : to) {
                User user = userRepository.findByUsername(username).orElse(null);
                if (user == null || !Boolean.TRUE.equals(user.getActive()) || SENDER_ROLES.contains(user.getRole()))
                    return ResponseEntity.badRequest().body(Map.of("error", "보낼 수 없는 사람입니다: " + username));
            }
            LocalDateTime now = LocalDateTime.now();
            for (String username : to) {
                UserRequest r = new UserRequest();
                setField(r, "fromUsername", auth.getName());
                setField(r, "toUsername",   username);
                setField(r, "content",      content);
                setField(r, "status",       "SENT");
                setField(r, "createdAt",    now);
                requestRepository.save(r);
            }
            return ResponseEntity.ok(Map.of("message", to.size() + "명에게 보냈습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    // ── 관리자: 삭제 (잘못 보낸 요청 취소) ──
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'requests')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        requestRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // ── 받는 사람: 아직 완료 안 된 내 요청 ──
    @GetMapping("/mine")
    public ResponseEntity<?> getMine(Authentication auth) {
        Map<String, String> names = new HashMap<>();
        Function<String, String> nameOf = u -> names.computeIfAbsent(u, k ->
                userRepository.findByUsername(k).map(x -> x.getName() != null ? x.getName() : k).orElse(k));
        return ResponseEntity.ok(requestRepository
                .findByToUsernameAndStatusNotOrderByCreatedAtAsc(auth.getName(), "DONE").stream()
                .map(r -> toMap(r, nameOf))
                .toList());
    }

    // ── 받는 사람: 확인 ──
    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication auth) {
        return changeStatus(id, auth, "READ");
    }

    // ── 받는 사람: 수정 완료 ──
    @PostMapping("/{id}/done")
    public ResponseEntity<?> markDone(@PathVariable Long id, Authentication auth) {
        return changeStatus(id, auth, "DONE");
    }

    private ResponseEntity<?> changeStatus(Long id, Authentication auth, String status) {
        UserRequest r = requestRepository.findById(id).orElse(null);
        if (r == null || !r.getToUsername().equals(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "요청을 찾을 수 없습니다."));
        try {
            LocalDateTime now = LocalDateTime.now();
            // 완료를 바로 눌러도 확인 시간은 남김
            if (r.getReadAt() == null) setField(r, "readAt", now);
            if ("DONE".equals(status)) setField(r, "doneAt", now);
            if (!"DONE".equals(r.getStatus())) setField(r, "status", status);
            requestRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "저장되었습니다."));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    private Map<String, Object> toMap(UserRequest r, Function<String, String> nameOf) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id",           r.getId());
        m.put("fromUsername", r.getFromUsername());
        m.put("fromName",     Optional.ofNullable(nameOf.apply(r.getFromUsername())).orElse(r.getFromUsername()));
        m.put("toUsername",   r.getToUsername());
        m.put("toName",       Optional.ofNullable(nameOf.apply(r.getToUsername())).orElse(r.getToUsername()));
        m.put("content",      r.getContent());
        m.put("status",       r.getStatus());
        m.put("createdAt",    r.getCreatedAt().toString());
        m.put("readAt",       r.getReadAt() != null ? r.getReadAt().toString() : "");
        m.put("doneAt",       r.getDoneAt() != null ? r.getDoneAt().toString() : "");
        return m;
    }

    private void setField(Object obj, String fn, Object val) throws Exception { Field f = obj.getClass().getDeclaredField(fn); f.setAccessible(true); f.set(obj, val); }
}
