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

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}