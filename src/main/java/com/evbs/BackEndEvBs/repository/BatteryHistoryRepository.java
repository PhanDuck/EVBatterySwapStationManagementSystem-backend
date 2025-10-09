package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.BatteryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatteryHistoryRepository extends JpaRepository<BatteryHistory, Long> {

    // Tìm history theo battery
    List<BatteryHistory> findByBatteryId(Long batteryId);

    // Tìm history theo event type
    List<BatteryHistory> findByEventType(String eventType);

    // Tìm history theo station
    List<BatteryHistory> findByRelatedStationId(Long stationId);

    // Tìm history theo vehicle
    List<BatteryHistory> findByRelatedVehicleId(Long vehicleId);

    // Tìm history theo staff
    List<BatteryHistory> findByStaffId(Long staffId);
}