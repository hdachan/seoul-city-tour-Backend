//package com.example.seoulcitytour.config;
//
//import com.example.seoulcitytour.entity.TabPermission;
//import com.example.seoulcitytour.entity.User;
//import com.example.seoulcitytour.entity.UserRole;
//import com.example.seoulcitytour.repository.TabPermissionRepository;
//import com.example.seoulcitytour.repository.UserRepository;
//import com.example.seoulcitytour.repository.UserRoleRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class DataInitializer implements ApplicationRunner {
//
//    private final UserRepository          userRepository;
//    private final UserRoleRepository      userRoleRepository;
//    private final TabPermissionRepository tabPermissionRepository;
//    private final PasswordEncoder         passwordEncoder;
//
//    @Override
//    @Transactional
//    public void run(ApplicationArguments args) throws Exception {
//        // 역할/권한 초기화는 user_role 테이블 기준으로만 체크 (유저와 독립)
//        initRolesAndPermissions();
//        // 계정 초기화는 users 테이블 기준으로만 체크 (역할과 독립)
//        initDefaultUsers();
//    }
//
//    private void initRolesAndPermissions() throws Exception {
//        // user_role 테이블이 비어있으면 기본 역할 + 탭 권한 삽입
//        if (userRoleRepository.count() > 0) {
//            System.out.println("✅ 역할 데이터 이미 존재 - 초기화 스킵");
//            return;
//        }
//
//        record RoleDef(String key, String name, boolean system, List<String> tabs) {}
//
//        List<RoleDef> defaults = List.of(
//            new RoleDef("ROLE_ADMIN", "관리자", true,
//                List.of("admin","record","settlement","ginseng","guide-admin","sales-admin")),
//            new RoleDef("ROLE_DEV",  "개발자", true,
//                List.of("record","settlement","ginseng","guide-admin","sales-admin","dev")),
//            new RoleDef("ROLE_SALES","영업",   true,
//                List.of("sales")),
//            new RoleDef("ROLE_GUIDE","가이드", true,
//                List.of("guide-form"))
//        );
//
//        for (RoleDef def : defaults) {
//            // 역할 저장
//            UserRole role = new UserRole();
//            setField(role, "roleKey",     def.key());
//            setField(role, "displayName", def.name());
//            setField(role, "isSystem",    def.system());
//            setField(role, "active",      true);
//            userRoleRepository.save(role);
//
//            // 탭 권한 저장
//            for (String tabId : def.tabs()) {
//                TabPermission tp = new TabPermission();
//                setField(tp, "roleKey", def.key());
//                setField(tp, "tabId",   tabId);
//                tabPermissionRepository.save(tp);
//            }
//        }
//        System.out.println("✅ 기본 역할 및 탭 권한 초기화 완료");
//    }
//
//    private void initDefaultUsers() throws Exception {
//        // users 테이블이 비어있으면 테스트 계정 삽입
//        if (userRepository.count() > 0) {
//            System.out.println("✅ 유저 데이터 이미 존재 - 초기화 스킵");
//            return;
//        }
//
//        createUser("admin", "1234", "ROLE_ADMIN", "관리자");
//        createUser("sales", "1234", "ROLE_SALES", "영업담당자");
//        createUser("guide", "1234", "ROLE_GUIDE", "가이드");
//        createUser("dev",   "1234", "ROLE_DEV",   "개발자");
//        System.out.println("✅ 테스트 계정 4개 생성 완료");
//    }
//
//    private void createUser(String username, String password, String role, String name) throws Exception {
//        User user = new User();
//        setField(user, "username", username);
//        setField(user, "password", passwordEncoder.encode(password));
//        setField(user, "role",     role);
//        setField(user, "name",     name);
//        setField(user, "active",   true);
//        userRepository.save(user);
//    }
//
//    private void setField(Object obj, String fn, Object val) throws Exception {
//        java.lang.reflect.Field f = obj.getClass().getDeclaredField(fn);
//        f.setAccessible(true);
//        f.set(obj, val);
//    }
//}
