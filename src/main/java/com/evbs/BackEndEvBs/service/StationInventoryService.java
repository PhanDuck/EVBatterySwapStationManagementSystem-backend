package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.StationInventory;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.StationInventoryRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StationInventoryService {

    @Autowired
    private StationInventoryRepository stationInventoryRepository;

    @Autowired
    private BatteryRepository batteryRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private BatteryHealthService batteryHealthService;

    @Autowired
    private StaffStationAssignmentService staffStationAssignmentService;

    // ==================== WAREHOUSE QUERIES ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getAllBatteriesInWarehouseWithDetails() {
        List<StationInventory> inventories = stationInventoryRepository.findAll();

        List<Map<String, Object>> batteryDetails = inventories.stream()
                .map(this::mapToBatteryDetail)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("location", "WAREHOUSE");
        response.put("total", batteryDetails.size());
        response.put("batteries", batteryDetails);
        response.put("message", batteryDetails.isEmpty()
                ? "Kho không có pin nào"
                : "Có " + batteryDetails.size() + " pin trong kho");

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBatteriesNeedingMaintenanceInWarehouse() {
        List<Battery> batteriesInWarehouse = batteryHealthService.getBatteriesNeedingMaintenance().stream()
                .filter(b -> b.getCurrentStation() == null)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("location", "WAREHOUSE");
        response.put("total", batteriesInWarehouse.size());
        response.put("batteries", batteriesInWarehouse);
        response.put("message", batteriesInWarehouse.isEmpty()
                ? "Kho không có pin nào cần bảo trì"
                : "Kho có " + batteriesInWarehouse.size() + " pin cần bảo trì");

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAvailableBatteriesByType(Long batteryTypeId) {
        List<Battery> availableBatteries = batteryRepository.findByBatteryType_IdAndStatusAndCurrentStationIsNull(
                batteryTypeId, Battery.Status.AVAILABLE);

        Map<String, Object> response = new HashMap<>();
        response.put("batteryTypeId", batteryTypeId);
        response.put("location", "WAREHOUSE");
        response.put("total", availableBatteries.size());
        response.put("batteries", availableBatteries);
        response.put("message", availableBatteries.isEmpty()
                ? "Không có pin nào đang AVAILABLE trong kho với loại này"
                : "Tìm thấy " + availableBatteries.size() + " pin AVAILABLE trong kho cùng loại");

        return response;
    }

    // ==================== BATTERY TRANSFER OPERATIONS ====================

    @Transactional
    public Map<String, Object> moveBatteryToStation(Long batteryId, Long stationId, Long batteryTypeId) {
        // Validate staff access
        staffStationAssignmentService.validateStationAccess(stationId);

        Battery battery = getBatteryById(batteryId);
        Station station = getStationById(stationId);

        // Validate battery type matches station
        validateBatteryTypeMatchesStation(battery, station);

        // Validate other conditions
        validateBatteryForStationTransfer(battery, stationId, batteryTypeId);

        // KIỂM TRA: Không vượt quá capacity của trạm (giữ lại 1 slot trống)
        long currentBatteryCount = batteryRepository.findByCurrentStation_Id(stationId).size();
        if (currentBatteryCount >= station.getCapacity() - 1) {
            throw new IllegalStateException(
                String.format("Trạm '%s' đã đầy (%d/%d pin). Không thể chuyển thêm pin!", 
                    station.getName(), currentBatteryCount, station.getCapacity())
            );
        }

        // Move battery to station
        battery.setCurrentStation(station);
        batteryRepository.save(battery);

        // Remove from inventory
        removeBatteryFromInventory(batteryId);

        log.info("Đã chuyển pin {} đến trạm {}. Loại pin: {}",
                battery.getId(), station.getName(), battery.getBatteryType().getName());

        return createTransferResponse(battery, "KHO TỔNG", station.getName(), "Đã chuyển pin đến trạm thành công");
    }

    @Transactional
    public Map<String, Object> moveBatteryToWarehouse(Long batteryId, Long stationId) {
        // Validate staff access
        staffStationAssignmentService.validateStationAccess(stationId);

        Battery battery = getBatteryById(batteryId);
        Station station = getStationById(stationId);

        // Validate conditions
        validateBatteryForWarehouseTransfer(battery, stationId);

        String oldStationName = battery.getCurrentStation().getName();

        // Move battery to warehouse
        battery.setCurrentStation(null);
        batteryRepository.save(battery);

        // Add to inventory
        addBatteryToInventory(battery);

        log.info("Đã chuyển pin {} từ trạm {} về kho tổng", battery.getId(), oldStationName);

        return createTransferResponse(battery, oldStationName, "KHO TỔNG", "Đã chuyển pin về kho thành công");
    }

    // ==================== MAINTENANCE OPERATIONS ====================

    @Transactional
    public Map<String, Object> completeMaintenance(Long batteryId, Double newSOH) {
        if (newSOH == null || newSOH < 0 || newSOH > 100) {
            throw new IllegalArgumentException("SOH từ 0-100%!");
        }

        Battery battery = getBatteryById(batteryId);

        // Call battery health service to complete maintenance
        Map<String, Object> result = batteryHealthService.completeMaintenance(battery, BigDecimal.valueOf(newSOH));
        return result;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * VALIDATION: Kiểm tra pin phải cùng loại với trạm
     */
    private void validateBatteryTypeMatchesStation(Battery battery, Station station) {
        if (battery.getBatteryType() == null) {
            throw new IllegalStateException("Lỗi mạng!");
        }

        if (station.getBatteryType() == null) {
            throw new IllegalStateException("Lỗi mạng!");
        }

        if (!battery.getBatteryType().getId().equals(station.getBatteryType().getId())) {
            throw new IllegalStateException("Loại pin không tương thích!");
        }

        log.debug("Validation passed: Battery type {} matches station type {}",
                battery.getBatteryType().getName(), station.getBatteryType().getName());
    }

    /**
     * Map StationInventory to detailed battery information
     */
    private Map<String, Object> mapToBatteryDetail(StationInventory inv) {
        Map<String, Object> detail = new HashMap<>();
        Battery battery = inv.getBattery();

        // Inventory information
        detail.put("inventoryId", inv.getId());
        detail.put("inventoryStatus", inv.getStatus());
        detail.put("lastUpdate", inv.getLastUpdate());

        // Battery basic information
        detail.put("id", battery.getId());
        detail.put("model", battery.getModel());
        detail.put("capacity", battery.getCapacity());

        // Battery health information
        detail.put("stateOfHealth", battery.getStateOfHealth());
        detail.put("chargeLevel", battery.getChargeLevel());
        detail.put("lastChargedTime", battery.getLastChargedTime());

        // Battery status information
        detail.put("status", battery.getStatus());
        detail.put("manufactureDate", battery.getManufactureDate());
        detail.put("usageCount", battery.getUsageCount());
        detail.put("lastMaintenanceDate", battery.getLastMaintenanceDate());
        detail.put("createdAt", battery.getCreatedAt());
        detail.put("reservationExpiry", battery.getReservationExpiry());

        // Battery type information
        detail.put("batteryTypeId", battery.getBatteryType() != null ? battery.getBatteryType().getId() : null);
        detail.put("batteryTypeName", battery.getBatteryType() != null ? battery.getBatteryType().getName() : null);

        // Location information
        detail.put("currentStation", battery.getCurrentStation());

        return detail;
    }

    /**
     * Get battery by ID with exception handling
     */
    private Battery getBatteryById(Long batteryId) {
        return batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin với ID: " + batteryId));
    }

    /**
     * Get station by ID with exception handling
     */
    private Station getStationById(Long stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm với ID: " + stationId));
    }

    /**
     * Validate battery for station transfer
     */
    private void validateBatteryForStationTransfer(Battery battery, Long stationId, Long batteryTypeId) {
        // Check if battery is at station already
        if (battery.getCurrentStation() != null) {
            throw new IllegalStateException("Pin đã ở trạm!");
        }

        // Check battery status
        if (battery.getStatus() != Battery.Status.AVAILABLE) {
            throw new IllegalStateException("Pin không sẵn sàng!");
        }

        // Check battery type
        if (!battery.getBatteryType().getId().equals(batteryTypeId)) {
            throw new IllegalStateException("Loại pin không đúng!");
        }

        // Check battery health
        if (battery.getStateOfHealth() == null || battery.getStateOfHealth().compareTo(new BigDecimal("90.00")) < 0) {
            throw new IllegalStateException("Pin SOH < 90%!");
        }
    }

    /**
     * Validate battery for warehouse transfer
     */
    private void validateBatteryForWarehouseTransfer(Battery battery, Long stationId) {
        // Check if battery is at the specified station
        if (battery.getCurrentStation() == null || !battery.getCurrentStation().getId().equals(stationId)) {
            throw new IllegalStateException("Pin không ở trạm này!");
        }

        // Check battery status
        if (battery.getStatus() != Battery.Status.MAINTENANCE) {
            throw new IllegalStateException("Pin phải đang bảo trì!");
        }
    }

    /**
     * Add battery to inventory
     */
    private void addBatteryToInventory(Battery battery) {
        StationInventory inventory = new StationInventory();
        inventory.setBattery(battery);
        inventory.setStatus(StationInventory.Status.MAINTENANCE);
        inventory.setLastUpdate(LocalDateTime.now());
        stationInventoryRepository.save(inventory);

        log.debug("Đã thêm pin {} vào inventory", battery.getId());
    }

    /**
     * Remove battery from inventory
     */
    private void removeBatteryFromInventory(Long batteryId) {
        stationInventoryRepository.findAll().stream()
                .filter(inv -> inv.getBattery().getId().equals(batteryId))
                .findFirst()
                .ifPresent(stationInventoryRepository::delete);

        log.debug("Đã xóa pin {} khỏi inventory", batteryId);
    }

    /**
     * Create transfer response
     */
    private Map<String, Object> createTransferResponse(Battery battery, String fromLocation, String toLocation, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("batteryId", battery.getId());
        response.put("batteryModel", battery.getModel());
        response.put("batteryType", battery.getBatteryType().getName());
        response.put("fromLocation", fromLocation);
        response.put("toLocation", toLocation);
        response.put("status", battery.getStatus());
        response.put("stateOfHealth", battery.getStateOfHealth());
        response.put("movedAt", LocalDateTime.now());
        return response;
    }
}