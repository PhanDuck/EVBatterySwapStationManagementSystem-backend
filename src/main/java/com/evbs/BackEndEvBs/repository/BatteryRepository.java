package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatteryRepository extends JpaRepository<Battery, Long> {

    // Tìm battery theo station
    List<Battery> findByCurrentStation(Station station);

    // Tìm battery theo station ID
    List<Battery> findByCurrentStationId(Long stationId);

    // Tìm battery theo status
    List<Battery> findByStatus(String status);

    // Tìm available batteries tại station cụ thể
    List<Battery> findByCurrentStationAndStatus(Station station, String status);

    // Đếm số battery available tại station
    long countByCurrentStationAndStatus(Station station, String status);

    // Tìm battery theo model
    List<Battery> findByModelContainingIgnoreCase(String model);

    // Tìm battery với SOH >= giá trị
    @Query("SELECT b FROM Battery b WHERE b.stateOfHealth >= :minSoh")
    List<Battery> findByStateOfHealthGreaterThanEqual(@Param("minSoh") BigDecimal minSoh);

    // Tìm battery với capacity trong khoảng
    @Query("SELECT b FROM Battery b WHERE b.capacity BETWEEN :minCapacity AND :maxCapacity")
    List<Battery> findByCapacityBetween(@Param("minCapacity") BigDecimal minCapacity,
                                        @Param("maxCapacity") BigDecimal maxCapacity);
}