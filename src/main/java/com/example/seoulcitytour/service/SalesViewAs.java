package com.example.seoulcitytour.service;

import com.example.seoulcitytour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// 👤 영업 정산관리 – 관리자가 영업 사원 한 명의 💼 영업 정산 화면을 "그 사람 그대로" 쓸 때
//   요청 머리에 X-View-As: 영업사원아이디 를 붙이면 /api/sales-form 이 그 사람으로 동작
//   (잠금 · 미터기 · 저장 규칙이 영업 본인과 완전히 같음 → 그 사람이 겪는 오류를 그대로 재현)
//   영업 정산관리(sales-admin) 권한이 있는 사람만, 대상은 ROLE_SALES 사용자만
@Component("salesViewAs")
@RequiredArgsConstructor
public class SalesViewAs {

    public static final String HEADER = "X-View-As";

    private final TabPermissionService tabPermissionService;
    private final UserRepository       userRepository;

    private String target() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) return null;
        String t = attrs.getRequest().getHeader(HEADER);
        return t == null || t.isBlank() ? null : t.trim();
    }

    private boolean canViewAs(Authentication auth, String target) {
        return tabPermissionService.hasAccess(auth, "sales-admin")
                && userRepository.findByUsername(target)
                        .map(u -> "ROLE_SALES".equals(u.getRole())).orElse(false);
    }

    // @PreAuthorize 용: 평소엔 영업 정산(sales) 권한, 대신 보기면 영업 정산관리 권한
    public boolean allowed(Authentication auth) {
        String t = target();
        return t == null ? tabPermissionService.hasAccess(auth, "sales") : canViewAs(auth, t);
    }

    // 지금 요청의 "영업 사원" 아이디 (대신 보기면 그 사람, 아니면 로그인한 본인)
    public String who(Authentication auth) {
        String t = target();
        if (t == null) return auth.getName();
        if (!canViewAs(auth, t)) throw new AccessDeniedException("권한 없음");
        return t;
    }
}
