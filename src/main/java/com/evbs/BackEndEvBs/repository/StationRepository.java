package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    // Tìm station theo name
    List<Station> findByNameContainingIgnoreCase(String name);

    // Tìm station theo location
    List<Station> findByLocationContainingIgnoreCase(String location);

    // Tìm station theo status
    List<Station> findByStatus(Station.Status status);

    // Tìm station theo battery type
    List<Station> findByBatteryType(BatteryType batteryType);

    // Tìm station theo battery type và status
    List<Station> findByBatteryTypeAndStatus(BatteryType batteryType, Station.Status status);

    // Kiểm tra trùng tên station
    boolean existsByName(String name);

    // Tìm stations có pin AVAILABLE với health > minHealth và đúng batteryType
    // LOGIC MỚI: Tìm pin trực tiếp từ Battery.currentStation (không qua StationInventory)
    @Query("SELECT DISTINCT s FROM Station s " +
           "JOIN Battery b ON b.currentStation.id = s.id " +
           "WHERE s.batteryType = :batteryType " +
           "AND s.status = 'ACTIVE' " +
           "AND b.status = 'AVAILABLE' " +
           "AND b.stateOfHealth > :minHealth")
    List<Station> findStationsWithAvailableBatteries(
            @Param("batteryType") BatteryType batteryType, 
            @Param("minHealth") Integer minHealth);
}