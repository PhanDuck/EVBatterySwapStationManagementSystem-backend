package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.entity.StationInventory;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.BatteryRequest;
import com.evbs.BackEndEvBs.model.request.BatteryUpdateRequest;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.BatteryTypeRepository;
import com.evbs.BackEndEvBs.repository.StationInventoryRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatteryService {

    @Autowired
    private final BatteryRepository batteryRepository;

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final BatteryTypeRepository batteryTypeRepository;

    @Autowired
    private final StationInventoryRepository stationInventoryRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    /**
     * CREATE - Create new battery (Admin/Staff only)
     * Auto-add to StationInventory if battery not assigned to any station
     */
    @Transactional
    public Battery createBattery(BatteryRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        // VALIDATION: Không cho phép tạo pin với trạng thái IN_USE hoặc PENDING
        // Trong method
        if (request.getStatus() == Battery.Status.IN_USE || request.getStatus() == Battery.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New battery cannot be created with IN_USE or PENDING status");
        }

        // Validate battery type
        BatteryType batteryType = batteryTypeRepository.findById(request.getBatteryTypeId())
                .orElseThrow(() -> new NotFoundException("Battery type not found"));

        // Create battery manually to avoid ModelMapper conflicts
        Battery battery = new Battery();
        battery.setModel(request.getModel());
        battery.setCapacity(request.getCapacity());
        battery.setStateOfHealth(request.getStateOfHealth());
        battery.setManufactureDate(request.getManufactureDate());
        battery.setLastMaintenanceDate(request.getLastMaintenanceDate());
        battery.setBatteryType(batteryType);

        // Set status với validation mặc định
        Battery.Status status = request.getStatus() != null ? request.getStatus() : Battery.Status.AVAILABLE;

        // Double check validation
        if (status == Battery.Status.IN_USE || status == Battery.Status.PENDING) {
            status = Battery.Status.AVAILABLE; // Force to AVAILABLE if invalid
        }

        battery.setStatus(status);
        battery.setChargeLevel(BigDecimal.valueOf(100.0)); // Default: new battery = 100% charge

        // XÓA SET TRẠM - không set station nữa
        // if (request.getCurrentStationId() != null) {
        //     battery.setCurrentStation(stationRepository.findById(request.getCurrentStationId())
        //             .orElseThrow(() -> new NotFoundException("Station not found")));
        // }

        // Save battery first
        Battery savedBattery = batteryRepository.save(battery);

        // LUÔN LUÔN THÊM VÀO STATION INVENTORY - không có điều kiện
        StationInventory inventory = new StationInventory();
        inventory.setBattery(savedBattery);

        // Map Battery.Status -> StationInventory.Status
        if (savedBattery.getStatus() == Battery.Status.MAINTENANCE) {
            inventory.setStatus(StationInventory.Status.MAINTENANCE);
        } else {
            inventory.setStatus(StationInventory.Status.AVAILABLE);
        }

        inventory.setLastUpdate(LocalDateTime.now());
        stationInventoryRepository.save(inventory);

        return savedBattery;
    }

    /**
     * READ - Get all batteries (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Battery> getAllBatteries() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryRepository.findAll();
    }

    /**
     * READ - Lấy battery theo ID (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public Battery getBatteryById(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found"));
    }

    /**
     * READ - Lấy available batteries (Public)
     */
    @Transactional(readOnly = true)
    public List<Battery> getAvailableBatteries() {
        return batteryRepository.findByStatus(Battery.Status.AVAILABLE);
    }

    /**
     * READ - Lấy tất cả batteries trong station (Public)
     */
    @Transactional(readOnly = true)
    public List<Battery> getBatteriesByStation(Long stationId) {
        // Kiểm tra station có tồn tại không
        if (!stationRepository.existsById(stationId)) {
            throw new NotFoundException("Station not found");
        }
        return batteryRepository.findByCurrentStation_Id(stationId);
    }

    /**
     * UPDATE - Cập nhật battery (Admin/Staff only)
     */
    @Transactional
    public Battery updateBattery(Long id, BatteryUpdateRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        // Chỉ update những field không null
        if (request.getModel() != null && !request.getModel().trim().isEmpty()) {
            battery.setModel(request.getModel());
        }
        if (request.getCapacity() != null) {
            battery.setCapacity(request.getCapacity());
        }
        if (request.getStateOfHealth() != null) {
            battery.setStateOfHealth(request.getStateOfHealth());
        }
        if (request.getStatus() != null) {
            battery.setStatus(request.getStatus());
        }
        if (request.getManufactureDate() != null) {
            battery.setManufactureDate(request.getManufactureDate());
        }
        if (request.getLastMaintenanceDate() != null) {
            battery.setLastMaintenanceDate(request.getLastMaintenanceDate());
        }
        if (request.getBatteryTypeId() != null) {
            BatteryType batteryType = batteryTypeRepository.findById(request.getBatteryTypeId())
                    .orElseThrow(() -> new NotFoundException("Battery type not found"));
            battery.setBatteryType(batteryType);
        }
        if (request.getCurrentStationId() != null) {
            // Tìm station và set
            battery.setCurrentStation(stationRepository.findById(request.getCurrentStationId())
                    .orElseThrow(() -> new NotFoundException("Station not found")));
        }

        return batteryRepository.save(battery);
    }

    /**
     * DELETE - Xóa battery (Admin only)
     */
    @Transactional
    public void deleteBattery(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        battery.setStatus(Battery.Status.RETIRED);
        batteryRepository.save(battery);
    }

    /**
     * UPDATE - Tăng số lần sử dụng pin
     */
    @Transactional
    public Battery incrementBatteryUsage(Long batteryId) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        battery.incrementUsageCount();
        return batteryRepository.save(battery);
    }

    /**
     * UPDATE - Cập nhật ngày bảo trì
     */
    @Transactional
    public Battery updateMaintenanceDate(Long batteryId, LocalDate maintenanceDate) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        battery.setLastMaintenanceDate(maintenanceDate);
        return batteryRepository.save(battery);
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}