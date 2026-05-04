package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    // active=true 인 것만 조회
    List<User> findByActiveTrueOrderByNameAsc();
}
