package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.StationInventory;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.StationInventoryRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.evbs.BackEndEvBs.service.BatteryHealthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StationInventoryService {

    private final StationInventoryRepository stationInventoryRepository;
    private final BatteryRepository batteryRepository;
    private final StationRepository stationRepository;
    private final BatteryHealthService batteryHealthService;

    public StationInventoryService(StationInventoryRepository stationInventoryRepository,
                                  BatteryRepository batteryRepository,
                                  StationRepository stationRepository,
                                  BatteryHealthService batteryHealthService) {
        this.stationInventoryRepository = stationInventoryRepository;
        this.batteryRepository = batteryRepository;
        this.stationRepository = stationRepository;
        this.batteryHealthService = batteryHealthService;
    }

    @Transactional(readOnly = true)
    public List<StationInventory> getAllBatteriesInWarehouse() {
        return stationInventoryRepository.findAll();
    }

    /**
     * Lấy tất cả pin trong kho với thông tin đầy đủ
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllBatteriesInWarehouseWithDetails() {
        List<StationInventory> inventories = stationInventoryRepository.findAll();
        
        // Map sang response với đầy đủ thông tin battery
        List<Map<String, Object>> batteryDetails = inventories.stream()
            .map(inv -> {
                Map<String, Object> detail = new HashMap<>();
                Battery battery = inv.getBattery();
                
                // Thông tin từ StationInventory
                detail.put("inventoryId", inv.getId());
                detail.put("inventoryStatus", inv.getStatus());
                detail.put("lastUpdate", inv.getLastUpdate());
                
                // Thông tin đầy đủ từ Battery
                detail.put("id", battery.getId());
                detail.put("model", battery.getModel());
                detail.put("capacity", battery.getCapacity());
                detail.put("stateOfHealth", battery.getStateOfHealth());
                detail.put("chargeLevel", battery.getChargeLevel());
                detail.put("lastChargedTime", battery.getLastChargedTime());
                detail.put("status", battery.getStatus());
                detail.put("manufactureDate", battery.getManufactureDate());
                detail.put("usageCount", battery.getUsageCount());
                detail.put("lastMaintenanceDate", battery.getLastMaintenanceDate());
                detail.put("createdAt", battery.getCreatedAt());
                detail.put("reservationExpiry", battery.getReservationExpiry());
                detail.put("batteryTypeId", battery.getBatteryType() != null ? battery.getBatteryType().getId() : null);
                detail.put("batteryTypeName", battery.getBatteryType() != null ? battery.getBatteryType().getName() : null);
                detail.put("currentStation", battery.getCurrentStation()); // Sẽ là null vì ở trong kho
                
                return detail;
            })
            .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("location", "WAREHOUSE");
        response.put("total", batteryDetails.size());
        response.put("batteries", batteryDetails);
        response.put("message", batteryDetails.isEmpty() 
            ? "Kho không có pin nào" 
            : "Có " + batteryDetails.size() + " pin trong kho");
        
        return response;
    }





    /**
     * Chuyển pin từ kho đến trạm
     */
    @Transactional
    public Map<String, Object> moveBatteryToStation(Long batteryId, Long stationId, Long batteryTypeId) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin với ID: " + batteryId));
        
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm với ID: " + stationId));
        
        // Validate: Pin phải ở trong kho (không ở trạm nào)
        if (battery.getCurrentStation() != null) {
            throw new RuntimeException("Pin đã ở trạm: " + battery.getCurrentStation().getName());
        }
        
        // Validate: Pin phải AVAILABLE
        if (battery.getStatus() != Battery.Status.AVAILABLE) {
            throw new RuntimeException("Pin phải ở trạng thái AVAILABLE. Hiện tại: " + battery.getStatus());
        }
        
        // Validate: Station phải hỗ trợ BatteryType này
        if (!station.getBatteryType().getId().equals(batteryTypeId)) {
            String errorMsg = String.format("Trạm không hỗ trợ loại pin này. Trạm hỗ trợ: %s (ID: %d), Yêu cầu: BatteryType ID %d", 
                    station.getBatteryType().getName(), station.getBatteryType().getId(), batteryTypeId);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        
        // Validate: SOH phải >= 90%
        if (battery.getStateOfHealth() == null || 
            battery.getStateOfHealth().compareTo(new BigDecimal("90.00")) < 0) {
            throw new RuntimeException("Pin phải có SOH >= 90% để gửi đến trạm. SOH hiện tại: " + 
                     (battery.getStateOfHealth() != null ? battery.getStateOfHealth() + "%" : "N/A"));
        }
        
        // Chuyển pin đến trạm
        battery.setCurrentStation(station);
        batteryRepository.save(battery);
        
        // Xóa record khỏi StationInventory (vì không còn trong kho)
        stationInventoryRepository.findAll().stream()
                .filter(inv -> inv.getBattery().getId().equals(batteryId))
                .findFirst()
                .ifPresent(stationInventoryRepository::delete);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đã chuyển pin đến trạm thành công");
        response.put("batteryId", batteryId);
        response.put("batteryModel", battery.getModel());
        response.put("batteryType", battery.getBatteryType().getName());
        response.put("fromLocation", "KHO TỔNG");
        response.put("toStation", station.getName());
        response.put("stationId", stationId);
        response.put("status", battery.getStatus());
        response.put("stateOfHealth", battery.getStateOfHealth());
        response.put("movedAt", LocalDateTime.now());
        
        return response;
    }

    /**
     * Chuyển pin bảo trì từ trạm về kho
     */
    @Transactional
    public Map<String, Object> moveBatteryToWarehouse(Long batteryId, Long stationId) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin với ID: " + batteryId));
        
        // Validate: Pin phải ở trạm này
        if (battery.getCurrentStation() == null || !battery.getCurrentStation().getId().equals(stationId)) {
            throw new RuntimeException("Pin không ở trạm này");
        }
        
        // Validate: Pin phải MAINTENANCE
        if (battery.getStatus() != Battery.Status.MAINTENANCE) {
            throw new RuntimeException("Pin phải ở trạng thái MAINTENANCE mới được chuyển về kho");
        }
        
        String oldStationName = battery.getCurrentStation().getName();
        
        // Chuyển pin về kho
        battery.setCurrentStation(null);  // Không còn ở trạm
        batteryRepository.save(battery);
        
        // Tạo record trong StationInventory
        StationInventory inventory = new StationInventory();
        inventory.setBattery(battery);
        inventory.setStatus(StationInventory.Status.MAINTENANCE);
        inventory.setLastUpdate(LocalDateTime.now());
        stationInventoryRepository.save(inventory);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đã chuyển pin về kho thành công");
        response.put("batteryId", batteryId);
        response.put("batteryModel", battery.getModel());
        response.put("fromStation", oldStationName);
        response.put("toLocation", "KHO TỔNG");
        response.put("status", battery.getStatus());
        response.put("stateOfHealth", battery.getStateOfHealth());
        response.put("movedAt", LocalDateTime.now());
        
        return response;
    }

    /**
     * Lấy danh sách pin AVAILABLE trong kho cùng loại
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailableBatteriesByType(Long batteryTypeId) {
        // Tìm pin AVAILABLE + TRONG KHO (currentStation = null) + cùng BatteryType
        List<Battery> availableBatteriesInWarehouse = batteryRepository.findByBatteryType_IdAndStatusAndCurrentStationIsNull(
            batteryTypeId, 
            Battery.Status.AVAILABLE
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("batteryTypeId", batteryTypeId);
        response.put("location", "WAREHOUSE");
        response.put("total", availableBatteriesInWarehouse.size());
        response.put("batteries", availableBatteriesInWarehouse);
        response.put("message", availableBatteriesInWarehouse.isEmpty() 
            ? "Không có pin nào đang AVAILABLE trong kho với loại này" 
            : "Tìm thấy " + availableBatteriesInWarehouse.size() + " pin AVAILABLE trong kho cùng loại");
        
        return response;
    }

    /**
     * Hoàn thành bảo trì pin và cập nhật SOH
     */
    @Transactional
    public Map<String, Object> completeMaintenance(Long batteryId, Double newSOH) {
        if (newSOH == null || newSOH < 0 || newSOH > 100) {
            throw new RuntimeException("SOH phải từ 0-100%");
        }

        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found with ID: " + batteryId));
        
        // Kiểm tra pin phải ở trong kho (không ở trạm)
        if (battery.getCurrentStation() != null) {
            throw new RuntimeException("Chỉ có thể bảo trì pin đang ở trong kho. Pin này đang ở trạm: " + battery.getCurrentStation().getName());
        }
        
        BigDecimal newSOHValue = BigDecimal.valueOf(newSOH);
        batteryHealthService.completeMaintenance(battery, newSOHValue);
        
        Map<String, Object> response = new HashMap<>();
        
        // Message thay đổi theo status
        if (battery.getStatus() == Battery.Status.AVAILABLE) {
            response.put("message", "Bảo trì hoàn tất. Pin đã sẵn sàng sử dụng (SOH >= 70%).");
        } else {
            response.put("message", "SOH đã được cập nhật. Pin vẫn cần bảo trì (SOH < 70%).");
        }
        
        response.put("battery", battery);
        response.put("batteryId", batteryId);
        response.put("newSOH", newSOH);
        response.put("status", battery.getStatus());
        response.put("usageCount", battery.getUsageCount());
        
        return response;
    }
    @Transactional(readOnly = true)
    public Map<String, Object> getAllBatteriesInWarehouseWithDetails() {
        List<StationInventory> inventories = stationInventoryRepository.findAll();

        // Map sang response với đầy đủ thông tin battery
        List<Map<String, Object>> batteryDetails = inventories.stream()
                .map(inv -> {
                    Map<String, Object> detail = new HashMap<>();
                    Battery battery = inv.getBattery();

                    // Thông tin từ StationInventory
                    detail.put("inventoryId", inv.getId());
                    detail.put("inventoryStatus", inv.getStatus());
                    detail.put("lastUpdate", inv.getLastUpdate());

                    // Thông tin đầy đủ từ Battery
                    detail.put("id", battery.getId());
                    detail.put("model", battery.getModel());
                    detail.put("capacity", battery.getCapacity());
                    detail.put("stateOfHealth", battery.getStateOfHealth());
                    detail.put("chargeLevel", battery.getChargeLevel());
                    detail.put("lastChargedTime", battery.getLastChargedTime());
                    detail.put("status", battery.getStatus());
                    detail.put("manufactureDate", battery.getManufactureDate());
                    detail.put("usageCount", battery.getUsageCount());
                    detail.put("lastMaintenanceDate", battery.getLastMaintenanceDate());
                    detail.put("createdAt", battery.getCreatedAt());
                    detail.put("reservationExpiry", battery.getReservationExpiry());
                    detail.put("batteryTypeId", battery.getBatteryType() != null ? battery.getBatteryType().getId() : null);
                    detail.put("batteryTypeName", battery.getBatteryType() != null ? battery.getBatteryType().getName() : null);
                    detail.put("currentStation", battery.getCurrentStation()); // Sẽ là null vì ở trong kho

                    return detail;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("location", "WAREHOUSE");
        response.put("total", batteryDetails.size());
        response.put("batteries", batteryDetails);
        response.put("message", batteryDetails.isEmpty()
                ? "Kho không có pin nào"
                : "Có " + batteryDetails.size() + " pin trong kho");

        return response;
    }
}
