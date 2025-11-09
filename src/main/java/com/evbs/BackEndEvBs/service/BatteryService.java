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
import com.evbs.BackEndEvBs.repository.VehicleRepository;
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
    private final VehicleRepository vehicleRepository;

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tạo pin mới với trạng thái ĐANG SỬ DỤNG hoặc ĐANG CHỜ XỬ LÝ");
        }

        // Validate battery type
        BatteryType batteryType = batteryTypeRepository.findById(request.getBatteryTypeId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));

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
            throw new AuthenticationException("Truy cập bị từ chối");
        }
        // Sử dụng JOIN FETCH để tránh N+1 query problem khi load stationName và batteryTypeName
        return batteryRepository.findAllWithDetails();
    }

    /**
     * READ - Lấy tất cả batteries trong station (Public)
     */
    @Transactional(readOnly = true)
    public List<Battery> getBatteriesByStation(Long stationId) {
        // Kiểm tra station có tồn tại không
        if (!stationRepository.existsById(stationId)) {
            throw new NotFoundException("Không tìm thấy trạm");
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
            throw new AuthenticationException("Truy cập bị từ chối");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin"));

        // Lưu trạng thái cũ để xử lý StationInventory
        com.evbs.BackEndEvBs.entity.Station oldStation = battery.getCurrentStation();

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
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy pin"));
            battery.setBatteryType(batteryType);
        }
        
        // XỬ LÝ CHUYỂN ĐỔI VỊ TRÍ PIN (KHO ↔ TRẠM)
        if (request.getCurrentStationId() != null) {
            com.evbs.BackEndEvBs.entity.Station newStation = stationRepository.findById(request.getCurrentStationId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));
            
            // Trường hợp 1: KHO → TRẠM (oldStation = null → newStation != null)
            if (oldStation == null) {
                // XÓA khỏi StationInventory
                stationInventoryRepository.findByBattery(battery).ifPresent(inventory -> {
                    stationInventoryRepository.delete(inventory);
                });
            }
            
            // Gán pin vào trạm mới
            battery.setCurrentStation(newStation);
            
        } else if (request.getCurrentStationId() == null && oldStation != null) {
            // Trường hợp 2: TRẠM → KHO (oldStation != null → newStation = null)
            // Chỉ xử lý nếu request EXPLICITLY set currentStationId = null
            // (Để tránh trường hợp không update station)
            
            // Set currentStation = null
            battery.setCurrentStation(null);
            
            // THÊM vào StationInventory
            StationInventory inventory = new StationInventory();
            inventory.setBattery(battery);
            inventory.setStatus(battery.getStatus() == Battery.Status.MAINTENANCE 
                ? StationInventory.Status.MAINTENANCE 
                : StationInventory.Status.AVAILABLE);
            inventory.setLastUpdate(LocalDateTime.now());
            stationInventoryRepository.save(inventory);
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
            throw new AuthenticationException("Truy cập bị từ chối");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin"));

        battery.setStatus(Battery.Status.RETIRED);
        batteryRepository.save(battery);
    }

    /**
     * READ - Lấy pin ở kho theo vehicle (Admin/Staff only)
     * Lấy pin khớp với loại pin của xe
     */
    @Transactional(readOnly = true)
    public List<Battery> getWarehouseBatteriesByVehicleId(Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối");
        }

        // Tìm xe và lấy battery type
        com.evbs.BackEndEvBs.entity.Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));

        Long batteryTypeId = vehicle.getBatteryType().getId();

        // Lấy pin AVAILABLE và có currentStation (trong kho) theo loại pin của xe
        return batteryRepository.findByBatteryType_IdAndStatusAndCurrentStationIsNotNull(
            batteryTypeId, 
            Battery.Status.AVAILABLE
        );
    }


    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}