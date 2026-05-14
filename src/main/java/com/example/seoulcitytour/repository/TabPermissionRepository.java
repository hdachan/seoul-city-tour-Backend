package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.TabPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TabPermissionRepository extends JpaRepository<TabPermission, Long> {

    List<TabPermission> findByRoleKey(String roleKey);

    // 특정 역할이 해당 탭에 접근 권한이 있는지 확인
    boolean existsByRoleKeyAndTabId(String roleKey, String tabId);

    @Modifying
    @Query("DELETE FROM TabPermission t WHERE t.roleKey = :roleKey")
    void deleteByRoleKey(@Param("roleKey") String roleKey);

    @Modifying
    @Query("DELETE FROM TabPermission t WHERE t.roleKey = :roleKey AND t.tabId = :tabId")
    void deleteByRoleKeyAndTabId(@Param("roleKey") String roleKey, @Param("tabId") String tabId);
}
