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

        // 현재 사용자의 역할 추출
        String role = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("");

        if (role.isEmpty()) return false;

        // ADMIN, DEV 는 항상 허용
        if ("ROLE_ADMIN".equals(role) || "ROLE_DEV".equals(role)) return true;

        // 그 외는 tab_permission 테이블에서 확인
        return tabPermissionRepository.existsByRoleKeyAndTabId(role, tabId);
    }
}
