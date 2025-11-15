package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
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
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Chỉ Admin!");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin"));

        // CHẶN UPDATE KHI PIN ĐANG SỬ DỤNG
        if (battery.getStatus() == Battery.Status.IN_USE) {
            throw new IllegalStateException("Pin đang lắp xe!");
        }

        if (battery.getStatus() == Battery.Status.PENDING) {
            throw new IllegalStateException("Pin đã đặt trước!");
        }

        // CHẶN ĐỔI LOẠI PIN KHI PIN Ở TRẠM
        if (request.getBatteryTypeId() != null && battery.getCurrentStation() != null) {
            throw new IllegalStateException("Pin đang ở trạm, không thể đổi loại pin! Chuyển về kho trước.");
        }

        // Lưu trạng thái cũ để xử lý StationInventory
        Station oldStation = battery.getCurrentStation();

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

        // KHÔNG CHO UPDATE currentStation (vị trí pin chỉ thay đổi qua SwapTransaction)

        return batteryRepository.save(battery);
    }

    /**
     * DELETE - Xóa battery (Admin only)
     * CHỈ CHO PHÉP XÓA PIN ĐANG Ở KHO (currentStation = NULL)
     */
    @Transactional
    public void deleteBattery(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Chỉ Admin!");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin"));

        // KIỂM TRA PIN PHẢI Ở KHO
        if (battery.getCurrentStation() != null) {
            throw new IllegalStateException("Pin đang ở trạm!");
        }

        if (battery.getStatus() == Battery.Status.IN_USE) {
            throw new IllegalStateException("Pin đang lắp xe!");
        }

        if (battery.getStatus() == Battery.Status.PENDING) {
            throw new IllegalStateException("Pin đã đặt trước!");
        }

        // Soft delete
        battery.setStatus(Battery.Status.RETIRED);
        batteryRepository.save(battery);
    }

    /**
     * READ - Lấy pin ở kho theo vehicle (Admin/Staff only)
     * Lấy pin khớp với loại pin của xe
     * Pin ở kho: currentStation = NULL và có trong StationInventory
     */
    @Transactional(readOnly = true)
    public List<Battery> getWarehouseBatteriesByVehicleId(Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối");
        }

        // Tìm xe và lấy battery type
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));

        Long batteryTypeId = vehicle.getBatteryType().getId();

        // Lấy pin AVAILABLE và currentStation = NULL (trong kho) theo loại pin của xe
        return batteryRepository.findByBatteryType_IdAndStatusAndCurrentStationIsNull(
                batteryTypeId,
                Battery.Status.AVAILABLE
        );
    }

    /**
     * SWAP FAULTY BATTERY - Đổi pin lỗi (chỉ áp dụng cho pin IN_USE)
     * Pin lỗi: Lấy từ xe (IN_USE) -> đưa về kho với status MAINTENANCE
     * Pin thay thế: Lấy từ kho (AVAILABLE + currentStation = NULL + có trong StationInventory) -> gắn lên xe
     */
    @Transactional
    public Battery swapFaultyBattery(Long vehicleId, Long replacementBatteryId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối");
        }

        // 1. Tìm xe
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));

        // 2. Lấy pin lỗi TỪ XE (pin IN_USE đang gắn trên xe)
        Battery faultyBattery = vehicle.getCurrentBattery();
        if (faultyBattery == null) {
            throw new IllegalStateException("Xe không có pin để đổi");
        }

        // 3. VALIDATE: Pin lỗi phải có status IN_USE (đang gắn trên xe)
        if (faultyBattery.getStatus() != Battery.Status.IN_USE) {
            throw new IllegalStateException("Chỉ có thể đổi pin đang sử dụng (IN_USE). Pin hiện tại: " + faultyBattery.getStatus());
        }

        // 4. VALIDATE: Pin lỗi không được có currentStation (vì đang ở trên xe)
        if (faultyBattery.getCurrentStation() != null) {
            throw new IllegalStateException("Pin lỗi đang có currentStation, không hợp lệ cho pin IN_USE trên xe");
        }

        // 5. Tìm pin thay thế TỪ KHO
        Battery replacementBattery = batteryRepository.findById(replacementBatteryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin thay thế"));

        // 6. VALIDATE: Pin thay thế phải ở trong KHO (currentStation = NULL và có trong StationInventory)
        if (replacementBattery.getCurrentStation() != null) {
            throw new IllegalStateException("Pin thay thế phải ở trong KHO (currentStation = NULL), không được ở trạm");
        }
        if(replacementBattery.getStatus() == Battery.Status.IN_USE) {
            throw new IllegalStateException("Pin thay thế đang ở trên xe khác");
        }

        // 7. VALIDATE: Pin thay thế phải có status AVAILABLE
        if (replacementBattery.getStatus() != Battery.Status.AVAILABLE) {
            throw new IllegalStateException("Pin thay thế phải có trạng thái AVAILABLE. Pin hiện tại: " + replacementBattery.getStatus());
        }

        // 8. VALIDATE: Pin thay thế phải có trong StationInventory (chứng minh đang ở kho)
        StationInventory replacementInventory = stationInventoryRepository.findByBattery(replacementBattery)
                .orElseThrow(() -> new IllegalStateException("Pin thay thế không có trong kho (không tìm thấy StationInventory)"));

        // 9. VALIDATE: Loại pin phải khớp
        if (!faultyBattery.getBatteryType().getId().equals(replacementBattery.getBatteryType().getId())) {
            throw new IllegalStateException("Loại pin thay thế không khớp với loại pin của xe");
        }

        // ========== BẮT ĐẦU THỰC HIỆN SWAP ==========

        // 10. Cập nhật PIN LỖI: Rời xe -> Vào kho với status MAINTENANCE
        faultyBattery.setStatus(Battery.Status.MAINTENANCE);
        faultyBattery.setCurrentStation(null);  // Vẫn ở kho (currentStation = NULL)
        faultyBattery.setLastMaintenanceDate(LocalDate.now());
        batteryRepository.save(faultyBattery);

        // 11. Tạo StationInventory cho pin lỗi (đưa vào kho)
        StationInventory faultyInventory = stationInventoryRepository.findByBattery(faultyBattery)
                .orElseGet(() -> {
                    StationInventory newInventory = new StationInventory();
                    newInventory.setBattery(faultyBattery);
                    return newInventory;
                });
        faultyInventory.setStatus(StationInventory.Status.MAINTENANCE);
        faultyInventory.setLastUpdate(LocalDateTime.now());
        stationInventoryRepository.save(faultyInventory);

        // 12. Cập nhật PIN THAY THẾ: Rời kho -> Lên xe với status IN_USE
        replacementBattery.setStatus(Battery.Status.IN_USE);
        replacementBattery.setCurrentStation(null);  // Không thuộc trạm nào (đang ở trên xe)
        replacementBattery.incrementUsageCount();
        batteryRepository.save(replacementBattery);

        // 13. Xóa StationInventory của pin thay thế (rời kho)
        stationInventoryRepository.delete(replacementInventory);

        // 14. Cập nhật XE: Tháo pin lỗi, gắn pin mới
        vehicle.setCurrentBattery(replacementBattery);
        vehicleRepository.save(vehicle);

        return replacementBattery;
    }


    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}