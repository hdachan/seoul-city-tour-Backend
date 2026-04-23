package com.example.seoulcitytour.repository;

import com.example.seoulcitytour.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatformRepository extends JpaRepository<Platform, Long> {
    List<Platform> findByActiveTrueOrderByNameAsc();
}
