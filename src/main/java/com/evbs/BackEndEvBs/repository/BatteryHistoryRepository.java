package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.BatteryHistory;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BatteryHistoryRepository extends JpaRepository<BatteryHistory, Long> {

    // Tìm history theo battery
    List<BatteryHistory> findByBatteryOrderByEventTimeDesc(Battery battery);

    // Tìm history theo battery ID
    List<BatteryHistory> findByBatteryIdOrderByEventTimeDesc(Long batteryId);

    // Tìm history theo event type
    List<BatteryHistory> findByEventTypeOrderByEventTimeDesc(String eventType);

    // Tìm history theo station
    List<BatteryHistory> findByRelatedStationOrderByEventTimeDesc(Station station);

    // Tìm history theo vehicle
    List<BatteryHistory> findByRelatedVehicleOrderByEventTimeDesc(Vehicle vehicle);

    // Tìm history theo staff
    List<BatteryHistory> findByStaffIdOrderByEventTimeDesc(Long staffId);

    // Tìm history trong khoảng thời gian
    List<BatteryHistory> findByEventTimeBetweenOrderByEventTimeDesc(LocalDateTime start, LocalDateTime end);

    // Tìm history của battery trong khoảng thời gian
    @Query("SELECT bh FROM BatteryHistory bh WHERE bh.battery.id = :batteryId AND bh.eventTime BETWEEN :start AND :end ORDER BY bh.eventTime DESC")
    List<BatteryHistory> findByBatteryIdAndEventTimeBetween(@Param("batteryId") Long batteryId,
                                                            @Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);

    // Lấy lịch sử gần nhất của battery
    @Query("SELECT bh FROM BatteryHistory bh WHERE bh.battery.id = :batteryId ORDER BY bh.eventTime DESC LIMIT 1")
    BatteryHistory findLatestByBatteryId(@Param("batteryId") Long batteryId);

    // Thống kê số lượng event theo type
    @Query("SELECT bh.eventType, COUNT(bh) FROM BatteryHistory bh GROUP BY bh.eventType")
    List<Object[]> countByEventType();
}