package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.UserRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRequestRepository extends JpaRepository<UserRequest, Long> {

    // 관리자 목록 (최신순)
    List<UserRequest> findAllByOrderByCreatedAtDesc();

    // 받는 사람: 아직 완료 안 된 요청 (오래된 순)
    List<UserRequest> findByToUsernameAndStatusNotOrderByCreatedAtAsc(String toUsername, String status);
}
