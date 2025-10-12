package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Battery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatteryRepository extends JpaRepository<Battery, Long> {

    // Tìm batteries theo model
    List<Battery> findByModelContainingIgnoreCase(String model);

    // Tìm batteries theo status
    List<Battery> findByStatus(Battery.Status status);

    // Tìm batteries theo station
    List<Battery> findByCurrentStationId(Long stationId);

    // Tìm available batteries
    List<Battery> findByStatusAndCurrentStationIsNotNull(Battery.Status status);
}