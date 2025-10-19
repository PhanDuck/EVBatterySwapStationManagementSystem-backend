package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Battery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BatteryRepository extends JpaRepository<Battery, Long> {

    // Tìm batteries theo model
    List<Battery> findByModelContainingIgnoreCase(String model);

    // Tìm batteries theo status
    List<Battery> findByStatus(Battery.Status status);

    // Tìm batteries theo station
    List<Battery> findByCurrentStation_Id(Long stationId);

    // Tìm batteries theo battery type
    List<Battery> findByBatteryType_Id(Long batteryTypeId);
    
    // ✅ Tìm pin AVAILABLE ở station với chargeLevel >= minCharge (cho swap transaction)
    // ORDER BY chargeLevel DESC để lấy pin đầy nhất trước
    @Query("SELECT b FROM Battery b WHERE b.currentStation.id = :stationId " +
           "AND b.status = :status " +
           "AND b.chargeLevel >= :minChargeLevel " +
           "ORDER BY b.chargeLevel DESC")
    List<Battery> findAvailableBatteriesAtStation(
            @Param("stationId") Long stationId,
            @Param("status") Battery.Status status,
            @Param("minChargeLevel") BigDecimal minChargeLevel
    );
}