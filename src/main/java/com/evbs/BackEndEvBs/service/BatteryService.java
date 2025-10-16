package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.BatteryRequest;
import com.evbs.BackEndEvBs.model.request.BatteryUpdateRequest;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.BatteryTypeRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    /**
     * CREATE - Tạo battery mới (Admin/Staff only)
     */
    @Transactional
    public Battery createBattery(BatteryRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        // Validate battery type
        BatteryType batteryType = batteryTypeRepository.findById(request.getBatteryTypeId())
                .orElseThrow(() -> new NotFoundException("Battery type not found"));

        Battery battery = modelMapper.map(request, Battery.class);
        battery.setBatteryType(batteryType);

        // Set current station nếu có
        if (request.getCurrentStationId() != null) {
            battery.setCurrentStation(stationRepository.findById(request.getCurrentStationId())
                    .orElseThrow(() -> new NotFoundException("Station not found")));
        }

        return batteryRepository.save(battery);
    }

    /**
     * READ - Lấy tất cả batteries (Admin/Staff only)
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
        batteryRepository.delete(battery);
    }

    /**
     * SEARCH - Tìm batteries theo model (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Battery> searchBatteriesByModel(String model) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryRepository.findByModelContainingIgnoreCase(model);
    }

    /**
     * UPDATE - Cập nhật battery status (Admin/Staff only)
     */
    @Transactional
    public Battery updateBatteryStatus(Long id, Battery.Status status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        battery.setStatus(status);
        return batteryRepository.save(battery);
    }

    /**
     * UPDATE - Cập nhật battery health (Admin/Staff only)
     */
    @Transactional
    public Battery updateBatteryHealth(Long id, BigDecimal stateOfHealth) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        battery.setStateOfHealth(stateOfHealth);
        return batteryRepository.save(battery);
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