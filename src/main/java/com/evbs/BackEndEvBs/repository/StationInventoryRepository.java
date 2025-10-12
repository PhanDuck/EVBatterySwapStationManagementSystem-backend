package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.StationInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationInventoryRepository extends JpaRepository<StationInventory, Long> {

    // Tìm inventory theo station
    List<StationInventory> findByStationId(Long stationId);

    // Tìm inventory theo battery
    Optional<StationInventory> findByBatteryId(Long batteryId);

    // Tìm available batteries trong station
    List<StationInventory> findByStationIdAndStatus(Long stationId, StationInventory.Status status);

    // Kiểm tra battery đã tồn tại trong inventory
    boolean existsByBatteryId(Long batteryId);
}