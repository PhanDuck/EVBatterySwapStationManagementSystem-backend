package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.BatteryHistoryRepository;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BatteryHistoryService {

    @Autowired
    private BatteryHistoryRepository batteryHistoryRepository;

    @Autowired
    private BatteryRepository batteryRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private AuthenticationService authenticationService;

    // ==================== READ ONLY OPERATIONS ====================

    // READ - Lấy tất cả battery history (Admin/Staff only)
    public List<BatteryHistory> getAllBatteryHistory() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return batteryHistoryRepository.findAll();
    }

    // READ - Lấy battery history theo ID (Admin/Staff only)
    public BatteryHistory getBatteryHistoryById(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        return batteryHistoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery history not found with id: " + id));
    }

    // READ - Lấy history của battery cụ thể (Admin/Staff only)
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

    // READ - Lấy history theo event type (Admin/Staff only)
    public List<BatteryHistory> getHistoryByEventType(String eventType) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return batteryHistoryRepository.findByEventTypeOrderByEventTimeDesc(eventType);
    }

    // READ - Lấy history theo station (Admin/Staff only)
    public List<BatteryHistory> getHistoryByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + stationId));

        return batteryHistoryRepository.findByRelatedStationOrderByEventTimeDesc(station);
    }

    // READ - Lấy history theo vehicle (Admin/Staff only)
    public List<BatteryHistory> getHistoryByVehicle(Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found with id: " + vehicleId));

        return batteryHistoryRepository.findByRelatedVehicleOrderByEventTimeDesc(vehicle);
    }

    // READ - Lấy history trong khoảng thời gian (Admin/Staff only)
    public List<BatteryHistory> getHistoryByTimeRange(LocalDateTime start, LocalDateTime end) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return batteryHistoryRepository.findByEventTimeBetweenOrderByEventTimeDesc(start, end);
    }

    // READ - Lấy lịch sử gần nhất của battery (Admin/Staff only)
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

    // READ - Thống kê event types (Admin/Staff only)
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

    // AUTO CREATE - Tạo history record khi có sự kiện (System)
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

        // Set staff if provided
        if (staffId != null) {
            // Note: You might need UserRepository here
            // For now, we'll skip staff assignment or you can implement it
        }

        return batteryHistoryRepository.save(history);
    }

    // AUTO CREATE - Khi battery được swap
    public BatteryHistory logBatterySwap(Long batteryId, Long fromStationId, Long toVehicleId, Long staffId) {
        return createBatteryHistory(batteryId, "SWAP_OUT", fromStationId, toVehicleId, staffId);
    }

    // AUTO CREATE - Khi battery được trả về station
    public BatteryHistory logBatteryReturn(Long batteryId, Long toStationId, Long fromVehicleId, Long staffId) {
        return createBatteryHistory(batteryId, "SWAP_IN", toStationId, fromVehicleId, staffId);
    }

    // AUTO CREATE - Khi battery bắt đầu sạc
    public BatteryHistory logBatteryChargingStart(Long batteryId, Long stationId, Long staffId) {
        return createBatteryHistory(batteryId, "CHARGING_START", stationId, null, staffId);
    }

    // AUTO CREATE - Khi battery kết thúc sạc
    public BatteryHistory logBatteryChargingEnd(Long batteryId, Long stationId, Long staffId) {
        return createBatteryHistory(batteryId, "CHARGING_END", stationId, null, staffId);
    }

    // AUTO CREATE - Khi battery được bảo trì
    public BatteryHistory logBatteryMaintenance(Long batteryId, Long stationId, Long staffId) {
        return createBatteryHistory(batteryId, "MAINTENANCE", stationId, null, staffId);
    }

    // AUTO CREATE - Khi battery được kiểm tra SOH
    public BatteryHistory logBatteryHealthCheck(Long batteryId, Long stationId, Long staffId) {
        return createBatteryHistory(batteryId, "HEALTH_CHECK", stationId, null, staffId);
    }

    // AUTO CREATE - Khi battery được retire
    public BatteryHistory logBatteryRetirement(Long batteryId, Long stationId, Long staffId) {
        return createBatteryHistory(batteryId, "RETIRED", stationId, null, staffId);
    }

    // Helper method kiểm tra role
    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}