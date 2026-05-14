// ── UserRoleRepository.java ──
package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByActiveTrueOrderByDisplayNameAsc();
    Optional<UserRole> findByRoleKey(String roleKey);
    boolean existsByRoleKey(String roleKey);
}
