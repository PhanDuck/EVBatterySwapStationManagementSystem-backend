package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.BatteryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatteryTypeRepository extends JpaRepository<BatteryType, Long> {

    // Tìm battery type theo name
    List<BatteryType> findByNameContainingIgnoreCase(String name);

    // Kiểm tra trùng tên
    boolean existsByName(String name);
}