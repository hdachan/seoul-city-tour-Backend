package com.example.seoulcitytour.controller;

import com.example.seoulcitytour.entity.TabPermission;
import com.example.seoulcitytour.entity.UserRole;
import com.example.seoulcitytour.repository.TabPermissionRepository;
import com.example.seoulcitytour.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevController {

    private final UserRoleRepository      userRoleRepository;
    private final TabPermissionRepository tabPermissionRepository;

    private static final List<Map<String, String>> ALL_TABS = List.of(
        Map.of("id", "record",      "label", "📋 운행 기록"),
        Map.of("id", "settlement",  "label", "💰 업체별 정산"),
        Map.of("id", "ginseng",     "label", "🌿 인삼 매출"),
        Map.of("id", "guide-admin", "label", "📂 가이드 정산관리"),
        Map.of("id", "sales-admin", "label", "📊 영업 정산관리"),
        Map.of("id", "guide-form",  "label", "📝 가이드 정산"),
        Map.of("id", "sales",       "label", "💼 영업 정산"),
        Map.of("id", "admin",       "label", "👥 계정 관리")
    );

    // ── 탭 목록 (ADMIN, DEV 모두 접근 가능) ──
    @GetMapping("/tabs")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<?> getTabs() {
        return ResponseEntity.ok(ALL_TABS);
    }

    // ── 역할 목록 (ADMIN도 읽기 가능 - 계정 관리에서 역할 목록 사용) ──
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public ResponseEntity<?> getRoles() {
        var roles = userRoleRepository.findByActiveTrueOrderByDisplayNameAsc();
        return ResponseEntity.ok(roles.stream().map(r -> {
            var tabs = tabPermissionRepository.findByRoleKey(r.getRoleKey())
                    .stream().map(TabPermission::getTabId).toList();
            return Map.of(
                    "id",          r.getId(),
                    "roleKey",     r.getRoleKey(),
                    "displayName", r.getDisplayName(),
                    "isSystem",    r.getIsSystem(),
                    "allowedTabs", tabs
            );
        }).toList());
    }

    // ── 역할 추가 (DEV만) ──
    @PostMapping("/roles")
    @PreAuthorize("hasRole('DEV')")
    public ResponseEntity<?> addRole(@RequestBody Map<String, Object> body) {
        try {
            String displayName = ((String) body.get("displayName")).trim();
            if (displayName.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "역할 이름을 입력해주세요."));

            String roleKey = "ROLE_CUSTOM_" + System.currentTimeMillis();
            UserRole role = new UserRole();
            setField(role, "roleKey",     roleKey);
            setField(role, "displayName", displayName);
            setField(role, "isSystem",    false);
            setField(role, "active",      true);
            userRoleRepository.save(role);

            @SuppressWarnings("unchecked")
            List<String> tabs = (List<String>) body.getOrDefault("allowedTabs", List.of());
            saveTabPermissions(roleKey, tabs);

            return ResponseEntity.ok(Map.of("message", "역할이 추가되었습니다.", "roleKey", roleKey));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "추가 실패: " + e.getMessage()));
        }
    }

    // ── 역할 수정 (DEV만) ──
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasRole('DEV')")
    @Transactional
    public ResponseEntity<?> updateRole(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        try {
            UserRole role = userRoleRepository.findById(id).orElseThrow();
            if (!role.getIsSystem()) {
                String displayName = ((String) body.get("displayName")).trim();
                setField(role, "displayName", displayName);
                userRoleRepository.save(role);
            }
            @SuppressWarnings("unchecked")
            List<String> tabs = (List<String>) body.getOrDefault("allowedTabs", List.of());
            tabPermissionRepository.deleteByRoleKey(role.getRoleKey());
            saveTabPermissions(role.getRoleKey(), tabs);
            return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "수정 실패: " + e.getMessage()));
        }
    }

    // ── 역할 삭제 (DEV만) ──
    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasRole('DEV')")
    @Transactional
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        try {
            UserRole role = userRoleRepository.findById(id).orElseThrow();
            if (role.getIsSystem())
                return ResponseEntity.badRequest().body(Map.of("error", "기본 역할은 삭제할 수 없습니다."));
            tabPermissionRepository.deleteByRoleKey(role.getRoleKey());
            setField(role, "active", false);
            userRoleRepository.save(role);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "삭제 실패: " + e.getMessage()));
        }
    }

    // ── 권한 조회 (로그인한 사용자 본인 역할) ──
    @GetMapping("/permissions/{roleKey}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPermissions(@PathVariable String roleKey) {
        var tabs = tabPermissionRepository.findByRoleKey(roleKey)
                .stream().map(TabPermission::getTabId).toList();
        return ResponseEntity.ok(Map.of("roleKey", roleKey, "allowedTabs", tabs));
    }

    private void saveTabPermissions(String roleKey, List<String> tabIds) throws Exception {
        for (String tabId : tabIds) {
            TabPermission tp = new TabPermission();
            setField(tp, "roleKey", roleKey);
            setField(tp, "tabId",   tabId);
            tabPermissionRepository.save(tp);
        }
    }

    private void setField(Object obj, String fn, Object val) throws Exception {
        Field f = obj.getClass().getDeclaredField(fn);
        f.setAccessible(true);
        f.set(obj, val);
    }
}
