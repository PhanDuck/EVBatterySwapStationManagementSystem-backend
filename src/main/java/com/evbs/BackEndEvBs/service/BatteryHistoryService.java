package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.BatteryHistoryRepository;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatteryHistoryService {

    @Autowired
    private final BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private final BatteryRepository batteryRepository;

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final VehicleRepository vehicleRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    // ==================== READ ONLY OPERATIONS ====================

    /**
     * READ - Lấy tất cả battery history (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getAllBatteryHistory() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return batteryHistoryRepository.findAll();
    }

    /**
     * READ - Lấy battery history theo ID (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public BatteryHistory getBatteryHistoryById(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        return batteryHistoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery history not found with id: " + id));
    }

    /**
     * READ - Lấy history của battery cụ thể (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getHistoryByBattery(Long batteryId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        // Verify battery exists
        if (!batteryRepository.existsById(batteryId)) {
            throw new NotFoundException("Battery not found with id: " + batteryId);
        }

        return batteryHistoryRepository.findByBatteryIdOrderByEventTimeDesc(batteryId);
    }

    /**
     * READ - Lấy history theo event type (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getHistoryByEventType(String eventType) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return batteryHistoryRepository.findByEventTypeOrderByEventTimeDesc(eventType);
    }

    /**
     * READ - Lấy history theo station (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getHistoryByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + stationId));

        return batteryHistoryRepository.findByRelatedStationOrderByEventTimeDesc(station);
    }

    /**
     * READ - Lấy history theo vehicle (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getHistoryByVehicle(Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found with id: " + vehicleId));

        return batteryHistoryRepository.findByRelatedVehicleOrderByEventTimeDesc(vehicle);
    }

    /**
     * READ - Lấy history trong khoảng thời gian (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getHistoryByTimeRange(LocalDateTime start, LocalDateTime end) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return batteryHistoryRepository.findByEventTimeBetweenOrderByEventTimeDesc(start, end);
    }

    /**
     * READ - Lấy lịch sử gần nhất của battery (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public BatteryHistory getLatestHistoryByBattery(Long batteryId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        if (!batteryRepository.existsById(batteryId)) {
            throw new NotFoundException("Battery not found with id: " + batteryId);
        }

        return batteryHistoryRepository.findLatestByBatteryId(batteryId);
    }

    /**
     * READ - Thống kê event types (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getEventTypeStatistics() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        List<Object[]> results = batteryHistoryRepository.countByEventType();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> (Long) result[1]
                ));
    }

    // ==================== AUTO CREATE OPERATIONS ====================

    /**
     * AUTO CREATE - Tạo history record khi có sự kiện (System)
     */
    @Transactional
    public BatteryHistory createBatteryHistory(Long batteryId, String eventType,
                                               Long stationId, Long vehicleId, Long staffId) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found with id: " + batteryId));

        BatteryHistory history = new BatteryHistory();
        history.setBattery(battery);
        history.setEventType(eventType);
        history.setEventTime(LocalDateTime.now());

        // Set related station if provided
        if (stationId != null) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new NotFoundException("Station not found with id: " + stationId));
            history.setRelatedStation(station);
        }

        // Set related vehicle if provided
        if (vehicleId != null) {
            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new NotFoundException("Vehicle not found with id: " + vehicleId));
            history.setRelatedVehicle(vehicle);
        }

        return batteryHistoryRepository.save(history);
    }

    /**
     * AUTO CREATE - Khi battery được swap
     */
    @Transactional
    public BatteryHistory logBatterySwap(Long batteryId, Long fromStationId, Long toVehicleId, Long staffId) {
        return createBatteryHistory(batteryId, "SWAP_OUT", fromStationId, toVehicleId, staffId);
    }

    /**
     * AUTO CREATE - Khi battery được trả về station
     */
    @Transactional
    public BatteryHistory logBatteryReturn(Long batteryId, Long toStationId, Long fromVehicleId, Long staffId) {
        return createBatteryHistory(batteryId, "SWAP_IN", toStationId, fromVehicleId, staffId);
    }

    /**
     * AUTO CREATE - Khi battery bắt đầu sạc
     */
    @Transactional
    public BatteryHistory logBatteryChargingStart(Long batteryId, Long stationId, Long staffId) {
        return createBatteryHistory(batteryId, "CHARGING_START", stationId, null, staffId);
    }

    /**
     * AUTO CREATE - Khi battery kết thúc sạc
     */
    @Transactional
    public BatteryHistory logBatteryChargingEnd(Long batteryId, Long stationId, Long staffId) {
        return createBatteryHistory(batteryId, "CHARGING_END", stationId, null, staffId);
    }

    /**
     * AUTO CREATE - Khi battery được bảo trì
     */
    @Transactional
    public BatteryHistory logBatteryMaintenance(Long batteryId, Long stationId, Long staffId) {
        return createBatteryHistory(batteryId, "MAINTENANCE", stationId, null, staffId);
    }

    /**
     * AUTO CREATE - Khi battery được kiểm tra SOH
     */
    @Transactional
    public BatteryHistory logBatteryHealthCheck(Long batteryId, Long stationId, Long staffId) {
        return createBatteryHistory(batteryId, "HEALTH_CHECK", stationId, null, staffId);
    }

    /**
     * AUTO CREATE - Khi battery được retire
     */
    @Transactional
    public BatteryHistory logBatteryRetirement(Long batteryId, Long stationId, Long staffId) {
        return createBatteryHistory(batteryId, "RETIRED", stationId, null, staffId);
    }

    /**
     * Helper method kiểm tra role
     */
    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}