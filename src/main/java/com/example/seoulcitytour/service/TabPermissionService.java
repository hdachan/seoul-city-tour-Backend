package com.example.seoulcitytour.service;

import com.example.seoulcitytour.repository.TabPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("tabPermissionService")
@RequiredArgsConstructor
public class TabPermissionService {

    private final TabPermissionRepository tabPermissionRepository;

    /**
     * 현재 로그인한 사용자의 역할이 해당 탭에 접근 권한이 있는지 확인
     * @PreAuthorize("@tabPermissionService.hasAccess(authentication, 'sales-admin')") 로 사용
     */
    public boolean hasAccess(Authentication authentication, String tabId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        String role = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("");

        // ← 이거 추가
        System.out.println(">>> hasAccess: role=[" + role + "], tabId=[" + tabId + "]");

        if (role.isEmpty()) return false;
        if ("ROLE_ADMIN".equals(role) || "ROLE_DEV".equals(role)) return true;

        boolean result = tabPermissionRepository.existsByRoleKeyAndTabId(role, tabId);

        // ← 이거 추가
        System.out.println(">>> result=[" + result + "]");

        return result;
    }
}
