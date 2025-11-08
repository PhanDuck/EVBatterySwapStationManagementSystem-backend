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

    // Tìm TẤT CẢ batteries với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT b FROM Battery b " +
           "LEFT JOIN FETCH b.currentStation " +
           "LEFT JOIN FETCH b.batteryType")
    List<Battery> findAllWithDetails();

    // Tìm batteries theo status với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT b FROM Battery b " +
           "LEFT JOIN FETCH b.currentStation " +
           "LEFT JOIN FETCH b.batteryType " +
           "WHERE b.status = :status")
    List<Battery> findByStatusWithDetails(@Param("status") Battery.Status status);

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

    // Dashboard queries - Đếm battery theo status
    Long countByStatus(Battery.Status status);

    // Đếm số battery đang ở trạm
    Long countByCurrentStationIsNotNull();

    // Tính mức sạc trung bình - Native query cho SQL Server
    @Query(value = "SELECT AVG(CAST(ChargeLevel AS FLOAT)) FROM Battery", nativeQuery = true)
    Double calculateAverageChargeLevel();

    // Tính sức khỏe trung bình - Native query cho SQL Server
    @Query(value = "SELECT AVG(CAST(StateOfHealth AS FLOAT)) FROM Battery", nativeQuery = true)
    Double calculateAverageHealthLevel();

    // Phân bố theo loại pin - Native query cho SQL Server
    @Query(value = "SELECT bt.Name, " +
            "COUNT(b.BatteryID), " +
            "SUM(CASE WHEN b.Status = 'AVAILABLE' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.Status = 'CHARGING' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.Status = 'MAINTENANCE' THEN 1 ELSE 0 END) " +
            "FROM Battery b " +
            "JOIN BatteryType bt ON b.BatteryTypeID = bt.BatteryTypeID " +
            "GROUP BY bt.Name",
            nativeQuery = true)
    List<Object[]> countBatteriesByType();
}