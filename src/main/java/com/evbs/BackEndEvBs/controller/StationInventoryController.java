package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.service.StaffStationAssignmentService;
import com.evbs.BackEndEvBs.service.StationInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/station-inventory")
@Tag(name = "Station Inventory Management")
public class StationInventoryController {

    @Autowired
    private StationInventoryService stationInventoryService;

    @Autowired
    private StaffStationAssignmentService staffStationAssignmentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get all batteries in warehouse",
               description = "Lấy tất cả pin đang ở trong kho (currentStation = null) và có record trong StationInventory")
    public ResponseEntity<Map<String, Object>> getAllBatteriesInWarehouse() {
        Map<String, Object> response = stationInventoryService.getAllBatteriesInWarehouseWithDetails();
        return ResponseEntity.ok(response);
    }
    // ==================== BATTERY MAINTENANCE ENDPOINTS ====================


    /**
     * POST /api/station-inventory/move-to-station : Chuyển pin từ kho đến trạm
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
        
        try {
            Map<String, Object> response = stationInventoryService.moveBatteryToStation(batteryId, stationId, batteryTypeId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
        
        try {
            Map<String, Object> response = stationInventoryService.completeMaintenance(batteryId, newSOH);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/station-inventory/move-to-warehouse : Staff chuyển pin bảo trì từ trạm về kho
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
        
        try {
            Map<String, Object> response = stationInventoryService.moveBatteryToWarehouse(batteryId, stationId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy hết danh sách pin đang ở trạng thái AVAILABLE, nằm TRONG KHO (không ở trạm nào), và cùng loại (BatteryType)
     */
    @GetMapping("/available-by-type/{batteryTypeId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get available batteries in warehouse by battery type",
               description = "Lấy danh sách pin đang ở trạng thái AVAILABLE, nằm TRONG KHO (không ở trạm nào), và cùng loại (BatteryType) với batteryTypeId đã nhập. " +
                             "Hữu ích khi cần tìm pin trong kho để gửi đến trạm.")
    public ResponseEntity<Map<String, Object>> getAvailableBatteriesByType(@PathVariable Long batteryTypeId) {
        Map<String, Object> response = stationInventoryService.getAvailableBatteriesByType(batteryTypeId);
        return ResponseEntity.ok(response);
    }
}
