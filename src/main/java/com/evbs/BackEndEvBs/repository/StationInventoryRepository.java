package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.StationInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationInventoryRepository extends JpaRepository<StationInventory, Long> {

    // Tìm inventory theo station (dùng station_id vì station là relationship)
    List<StationInventory> findByStation_Id(Long stationId);

    // Tìm inventory theo battery (dùng battery_id vì battery là relationship)
    Optional<StationInventory> findByBattery_Id(Long batteryId);

    // Tìm available batteries trong station
    List<StationInventory> findByStation_IdAndStatus(Long stationId, StationInventory.Status status);

    // Kiểm tra battery đã tồn tại trong inventory
    boolean existsByBattery_Id(Long batteryId);

    // Đếm số lượng battery trong station
    int countByStation_Id(Long stationId);

    // Kiểm tra battery có trong station cụ thể không
    boolean existsByStation_IdAndBattery_Id(Long stationId, Long batteryId);
}