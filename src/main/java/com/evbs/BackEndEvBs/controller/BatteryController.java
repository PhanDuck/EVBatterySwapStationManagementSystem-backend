package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.model.request.BatteryRequest;
import com.evbs.BackEndEvBs.model.request.BatteryUpdateRequest;
import com.evbs.BackEndEvBs.service.BatteryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/battery")
@SecurityRequirement(name = "api")
@Tag(name = "Battery Management")
public class BatteryController {

    @Autowired
    private BatteryService batteryService;

    // ==================== ADMIN/STAFF ENDPOINTS ====================

    /**
     * POST /api/battery : Create new battery (Admin/Staff only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Create new battery")
    public ResponseEntity<Battery> createBattery(@Valid @RequestBody BatteryRequest request) {
        Battery battery = batteryService.createBattery(request);
        return new ResponseEntity<>(battery, HttpStatus.CREATED);
    }

    /**
     * GET /api/battery : Get all batteries (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all batteries")
    public ResponseEntity<List<Battery>> getAllBatteries() {
        List<Battery> batteries = batteryService.getAllBatteries();
        return ResponseEntity.ok(batteries);
    }

    /**
     * PUT /api/battery/{id} : Update battery (Admin/Staff only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update battery")
    public ResponseEntity<Battery> updateBattery(
            @PathVariable Long id,
            @Valid @RequestBody BatteryUpdateRequest request) {
        Battery battery = batteryService.updateBattery(id, request);
        return ResponseEntity.ok(battery);
    }

    /**
     * DELETE /api/battery/{id} : Delete battery (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete battery")
    public ResponseEntity<Void> deleteBattery(@PathVariable Long id) {
        batteryService.deleteBattery(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/battery/warehouse/vehicle/{vehicleId} : Get warehouse batteries by vehicle (Admin/Staff only)
     * Lấy pin ở kho khớp với loại pin của xe
     */
    @GetMapping("/warehouse/vehicle/{vehicleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get warehouse batteries by vehicle")
    public ResponseEntity<List<Battery>> getWarehouseBatteriesByVehicle(@PathVariable Long vehicleId) {
        List<Battery> batteries = batteryService.getWarehouseBatteriesByVehicleId(vehicleId);
        return ResponseEntity.ok(batteries);
    }

}
