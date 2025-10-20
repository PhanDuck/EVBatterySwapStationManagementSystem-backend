package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.StationInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * StationInventoryRepository - Quản lý KHO TỔNG
 * 
 * StationInventory không còn liên kết với Station.
 * Chỉ quản lý pin trong kho (Battery.currentStation = NULL)
 */
@Repository
public interface StationInventoryRepository extends JpaRepository<StationInventory, Long> {

    // Tìm inventory theo battery
    Optional<StationInventory> findByBattery_Id(Long batteryId);

    // Tìm inventory theo status
    List<StationInventory> findByStatus(StationInventory.Status status);

    // Kiểm tra battery đã tồn tại trong kho
    boolean existsByBattery_Id(Long batteryId);
}