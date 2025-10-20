package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.StationInventory;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.evbs.BackEndEvBs.service.BatteryHealthService;
import com.evbs.BackEndEvBs.service.StaffStationAssignmentService;
import com.evbs.BackEndEvBs.service.StationInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/station-inventory")
@Tag(name = "Station Inventory Management")
public class StationInventoryController {

    @Autowired
    StationInventoryService stationInventoryService;

    @Autowired
    BatteryHealthService batteryHealthService;

    @Autowired
    BatteryRepository batteryRepository;

    @Autowired
    StationRepository stationRepository;

    @Autowired
    StaffStationAssignmentService staffStationAssignmentService;

    @Autowired
    com.evbs.BackEndEvBs.repository.StationInventoryRepository stationInventoryRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get all batteries in warehouse")
    public ResponseEntity<List<StationInventory>> getAllBatteriesInWarehouse() {
        List<StationInventory> batteries = stationInventoryService.getAllBatteriesInWarehouse();
        return ResponseEntity.ok(batteries);
    }

    /**
     * POST /api/station-inventory/replace : Thay pin bảo trì từ kho
     * 
     * @param maintenanceBatteryId ID pin bảo trì ở trạm
     * @param availableBatteryId ID pin thay thế từ kho
     * @param stationId ID trạm cần thay pin
     */
    @PostMapping("/replace")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Replace maintenance battery with battery from warehouse",
            description = "Thay pin bảo trì ở trạm bằng pin từ kho. Pin mới phải CÙNG BatteryType và health >= 90%")
    public ResponseEntity<Map<String, Object>> replaceBattery(
            @RequestParam Long maintenanceBatteryId,
            @RequestParam Long availableBatteryId,
            @RequestParam Long stationId) {
        Map<String, Object> result = stationInventoryService.replaceBatteryForMaintenance(
                maintenanceBatteryId, availableBatteryId, stationId);
        return ResponseEntity.ok(result);
    }

    // ==================== BATTERY MAINTENANCE ENDPOINTS ====================

    /**
     * GET /api/station-inventory/warehouse/needs-maintenance : Lấy pin cần bảo trì TRONG KHO
     * 
     * Pin trong kho: currentStation = NULL, có record trong StationInventory
     */
    @GetMapping("/warehouse/needs-maintenance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get batteries needing maintenance IN WAREHOUSE (SOH < 70%)",
               description = "Lấy danh sách pin cần bảo trì đang nằm trong kho tổng (không ở trạm nào)")
    public ResponseEntity<Map<String, Object>> getBatteriesNeedingMaintenanceInWarehouse() {
        // Lấy tất cả pin có SOH < 70%
        List<Battery> allBatteriesNeedMaintenance = batteryHealthService.getBatteriesNeedingMaintenance();
        
        // Filter chỉ lấy pin TRONG KHO (currentStation = NULL)
        List<Battery> batteriesInWarehouse = allBatteriesNeedMaintenance.stream()
                .filter(b -> b.getCurrentStation() == null)  // Pin không ở trạm nào
                .collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("location", "WAREHOUSE");
        response.put("total", batteriesInWarehouse.size());
        response.put("batteries", batteriesInWarehouse);
        response.put("message", batteriesInWarehouse.isEmpty() 
            ? "Kho không có pin nào cần bảo trì" 
            : "Kho có " + batteriesInWarehouse.size() + " pin cần bảo trì");
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/station-inventory/move-to-station : Chuyển pin từ kho đến trạm
     * 
     * @param batteryId ID pin cần chuyển
     * @param stationId ID trạm đích
     * @param batteryTypeId ID loại pin mà trạm cần (để kiểm tra type khớp)
     * Staff chỉ được gửi pins đến stations mình quản lý
     */
    @PostMapping("/move-to-station")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Move battery from warehouse to station",
            description = "Staff/Admin chuyển pin từ kho đến trạm. Staff chỉ được gửi pins đến stations mình quản lý. " +
                          "Validation: Pin phải AVAILABLE, trong kho, cùng BatteryType với yêu cầu, và SOH >= 90%.")
    public ResponseEntity<Map<String, Object>> moveBatteryToStation(
            @RequestParam Long batteryId,
            @RequestParam Long stationId,
            @RequestParam Long batteryTypeId) {
        
        // Validate station access for staff
        staffStationAssignmentService.validateStationAccess(stationId);
        
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin với ID: " + batteryId));
        
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm với ID: " + stationId));
        
        // Validate: Pin phải ở trong kho (không ở trạm nào)
        if (battery.getCurrentStation() != null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Pin đã ở trạm: " + battery.getCurrentStation().getName()
            ));
        }
        
        // Validate: Pin phải AVAILABLE (không được MAINTENANCE hoặc IN_USE)
        if (battery.getStatus() != Battery.Status.AVAILABLE) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Pin phải ở trạng thái AVAILABLE. Hiện tại: " + battery.getStatus()
            ));
        }
        
        // Validate: Pin phải cùng BatteryType
        if (!battery.getBatteryType().getId().equals(batteryTypeId)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Pin không đúng loại. Yêu cầu: BatteryType ID " + batteryTypeId + 
                         ", Pin hiện tại: " + battery.getBatteryType().getName() + " (ID: " + battery.getBatteryType().getId() + ")"
            ));
        }
        
        // Validate: SOH phải >= 90%
        if (battery.getStateOfHealth() == null || 
            battery.getStateOfHealth().compareTo(new BigDecimal("90.00")) < 0) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Pin phải có SOH >= 90% để gửi đến trạm. SOH hiện tại: " + 
                         (battery.getStateOfHealth() != null ? battery.getStateOfHealth() + "%" : "N/A")
            ));
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
        response.put("movedAt", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/station-inventory/{batteryId}/complete-maintenance : Hoàn thành bảo trì pin
     * Request param: ?newSOH=95.0
     * Logic tự động: SOH >= 70% → AVAILABLE, SOH < 70% → MAINTENANCE
     */
    @PatchMapping("/{batteryId}/complete-maintenance")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Complete battery maintenance and update SOH",
               description = "Admin cập nhật SOH mới cho pin trong kho. " +
                             "Nếu SOH >= 70%: Pin tự động chuyển sang AVAILABLE. " +
                             "Nếu SOH < 70%: Pin vẫn giữ MAINTENANCE.")
    public ResponseEntity<Map<String, Object>> completeMaintenance(
            @PathVariable @io.swagger.v3.oas.annotations.Parameter(description = "ID của pin cần cập nhật SOH", example = "123") Long batteryId,
            @RequestParam @io.swagger.v3.oas.annotations.Parameter(description = "SOH mới sau khi bảo trì (0-100)", example = "95.0") Double newSOH) {
        
        if (newSOH == null || newSOH < 0 || newSOH > 100) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "SOH phải từ 0-100%"
            ));
        }

        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found with ID: " + batteryId));
        
        // Kiểm tra pin phải ở trong kho (không ở trạm)
        if (battery.getCurrentStation() != null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Chỉ có thể bảo trì pin đang ở trong kho. Pin này đang ở trạm: " + battery.getCurrentStation().getName(),
                "batteryId", batteryId,
                "currentStation", battery.getCurrentStation().getName()
            ));
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
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/station-inventory/move-to-warehouse : Staff chuyển pin bảo trì từ trạm về kho
     * 
     * @param batteryId ID pin cần chuyển
     * @param stationId ID trạm hiện tại của pin
     * Staff chỉ được chuyển pins từ stations mình quản lý
     */
    @PostMapping("/move-to-warehouse")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Move maintenance battery from station to warehouse",
            description = "Staff chuyển pin bảo trì từ trạm về kho tổng. Staff chỉ được chuyển pins từ stations mình quản lý. Pin phải có status MAINTENANCE và đang ở trạm.")
    public ResponseEntity<Map<String, Object>> moveBatteryToWarehouse(
            @RequestParam Long batteryId,
            @RequestParam Long stationId) {
        
        // Validate station access for staff
        staffStationAssignmentService.validateStationAccess(stationId);
        
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin với ID: " + batteryId));
        
        // Validate: Pin phải ở trạm này
        if (battery.getCurrentStation() == null || !battery.getCurrentStation().getId().equals(stationId)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Pin không ở trạm này"
            ));
        }
        
        // Validate: Pin phải MAINTENANCE
        if (battery.getStatus() != Battery.Status.MAINTENANCE) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Pin phải ở trạng thái MAINTENANCE mới được chuyển về kho"
            ));
        }
        
        String oldStationName = battery.getCurrentStation().getName();
        
        // Chuyển pin về kho
        battery.setCurrentStation(null);  // Không còn ở trạm
        batteryRepository.save(battery);
        
        // Tạo record trong StationInventory
        StationInventory inventory = new StationInventory();
        inventory.setBattery(battery);
        inventory.setStatus(StationInventory.Status.MAINTENANCE);
        inventory.setLastUpdate(java.time.LocalDateTime.now());
        stationInventoryRepository.save(inventory);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đã chuyển pin về kho thành công");
        response.put("batteryId", batteryId);
        response.put("batteryModel", battery.getModel());
        response.put("fromStation", oldStationName);
        response.put("toLocation", "KHO TỔNG");
        response.put("status", battery.getStatus());
        response.put("stateOfHealth", battery.getStateOfHealth());
        response.put("movedAt", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/station-inventory/available-by-type/{batteryTypeId} : Lấy danh sách pin AVAILABLE TRONG KHO cùng loại
     * 
     * @param batteryTypeId ID loại pin cần tìm
     * @return Danh sách pin đang AVAILABLE, TRONG KHO (currentStation = null), và cùng BatteryType
     */
    @GetMapping("/available-by-type/{batteryTypeId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get available batteries in warehouse by battery type",
               description = "Lấy danh sách pin đang ở trạng thái AVAILABLE, nằm TRONG KHO (không ở trạm nào), và cùng loại (BatteryType) với batteryTypeId đã nhập. " +
                             "Hữu ích khi cần tìm pin trong kho để gửi đến trạm.")
    public ResponseEntity<Map<String, Object>> getAvailableBatteriesByType(@PathVariable Long batteryTypeId) {
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
        
        return ResponseEntity.ok(response);
    }
}
