package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final UserRepository userRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    /**
     * READ - Lấy tất cả battery history (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getAllBatteryHistory() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryHistoryRepository.findAll();
    }

    /**
     * READ - Lấy history theo battery (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getBatteryHistoryByBattery(Long batteryId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryHistoryRepository.findByBatteryId(batteryId);
    }

    /**
     * READ - Lấy history theo event type (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getBatteryHistoryByEventType(String eventType) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryHistoryRepository.findByEventType(eventType);
    }

    /**
     * READ - Lấy history theo station (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getBatteryHistoryByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryHistoryRepository.findByRelatedStationId(stationId);
    }

    /**
     * READ - Lấy history theo vehicle (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getBatteryHistoryByVehicle(Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryHistoryRepository.findByRelatedVehicleId(vehicleId);
    }

    /**
     * READ - Lấy history của staff hiện tại (Staff/Admin only)
     */
    @Transactional(readOnly = true)
    public List<BatteryHistory> getMyBatteryHistory() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryHistoryRepository.findByStaffId(currentUser.getId());
    }

    // ==================== AUTOMATIC HISTORY CREATION ====================

    /**
     * Tạo history khi battery được thêm vào station
     */
    @Transactional
    public BatteryHistory logBatteryAddedToStation(Long batteryId, Long stationId, Long staffId) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        BatteryHistory history = new BatteryHistory();
        history.setBattery(battery);
        history.setEventType("ADDED_TO_STATION");
        history.setEventTime(LocalDateTime.now());
        history.setRelatedStation(station);
        history.setStaff(staff);

        return batteryHistoryRepository.save(history);
    }

    /**
     * Tạo history khi battery được lấy ra từ station
     */
    @Transactional
    public BatteryHistory logBatteryRemovedFromStation(Long batteryId, Long stationId, Long staffId) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        BatteryHistory history = new BatteryHistory();
        history.setBattery(battery);
        history.setEventType("REMOVED_FROM_STATION");
        history.setEventTime(LocalDateTime.now());
        history.setRelatedStation(station);
        history.setStaff(staff);

        return batteryHistoryRepository.save(history);
    }

    /**
     * Tạo history khi battery được swap vào vehicle
     */
    @Transactional
    public BatteryHistory logBatterySwappedToVehicle(Long batteryId, Long vehicleId, Long staffId) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        BatteryHistory history = new BatteryHistory();
        history.setBattery(battery);
        history.setEventType("SWAPPED_TO_VEHICLE");
        history.setEventTime(LocalDateTime.now());
        history.setRelatedVehicle(vehicle);
        history.setStaff(staff);

        return batteryHistoryRepository.save(history);
    }

    /**
     * Tạo history khi battery được swap từ vehicle
     */
    @Transactional
    public BatteryHistory logBatterySwappedFromVehicle(Long batteryId, Long vehicleId, Long staffId) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        BatteryHistory history = new BatteryHistory();
        history.setBattery(battery);
        history.setEventType("SWAPPED_FROM_VEHICLE");
        history.setEventTime(LocalDateTime.now());
        history.setRelatedVehicle(vehicle);
        history.setStaff(staff);

        return batteryHistoryRepository.save(history);
    }

    /**
     * Tạo history khi battery được bảo trì
     */
    @Transactional
    public BatteryHistory logBatteryMaintenance(Long batteryId, Long staffId, String notes) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        BatteryHistory history = new BatteryHistory();
        history.setBattery(battery);
        history.setEventType("MAINTENANCE");
        history.setEventTime(LocalDateTime.now());
        history.setStaff(staff);

        return batteryHistoryRepository.save(history);
    }

    /**
     * Tạo history khi battery status thay đổi
     */
    @Transactional
    public BatteryHistory logBatteryStatusChange(Long batteryId, Long staffId, Battery.Status oldStatus, Battery.Status newStatus) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        BatteryHistory history = new BatteryHistory();
        history.setBattery(battery);
        history.setEventType("STATUS_CHANGE");
        history.setEventTime(LocalDateTime.now());
        history.setStaff(staff);

        return batteryHistoryRepository.save(history);
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}