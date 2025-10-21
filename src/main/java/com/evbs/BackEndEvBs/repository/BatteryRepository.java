package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatteryRepository extends JpaRepository<Battery, Long> {

    // Tìm batteries theo status
    List<Battery> findByStatus(Battery.Status status);

    // Tìm batteries theo station
    List<Battery> findByCurrentStation_Id(Long stationId);
    
    // Find AVAILABLE batteries in warehouse by type (currentStation = NULL)
    List<Battery> findByBatteryType_IdAndStatusAndCurrentStationIsNull(Long batteryTypeId, Battery.Status status);
    
    // Find reserved (PENDING) battery for a specific booking
    Optional<Battery> findByStatusAndReservedForBooking(Battery.Status status, Booking booking);
    
    // Find AVAILABLE batteries at station with chargeLevel >= minCharge (for swap)
    // ORDER BY chargeLevel DESC to get fullest battery first
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