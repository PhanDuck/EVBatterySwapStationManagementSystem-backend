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
     * GET /api/battery/{id} : Get battery by ID (Admin/Staff only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get battery by ID")
    public ResponseEntity<Battery> getBatteryById(@PathVariable Long id) {
        Battery battery = batteryService.getBatteryById(id);
        return ResponseEntity.ok(battery);
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

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * GET /api/battery/available : Get available batteries (Public)
     */
    @GetMapping("/available")
    @Operation(summary = "Get available batteries")
    public ResponseEntity<List<Battery>> getAvailableBatteries() {
        List<Battery> batteries = batteryService.getAvailableBatteries();
        return ResponseEntity.ok(batteries);
    }

    // ==================== UTILITY ENDPOINTS ====================

    /**
     * GET /api/battery/search/model : Search batteries by model (Admin/Staff only)
     */
    @GetMapping("/search/model")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Search batteries by model")
    public ResponseEntity<List<Battery>> searchBatteriesByModel(@RequestParam String model) {
        List<Battery> batteries = batteryService.searchBatteriesByModel(model);
        return ResponseEntity.ok(batteries);
    }

    /**
     * PATCH /api/battery/{id}/status : Update battery status (Admin/Staff only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update battery status")
    public ResponseEntity<Battery> updateBatteryStatus(
            @PathVariable Long id,
            @RequestParam Battery.Status status) {
        Battery battery = batteryService.updateBatteryStatus(id, status);
        return ResponseEntity.ok(battery);
    }

    /**
     * PATCH /api/battery/{id}/health : Update battery health (Admin/Staff only)
     */
    @PatchMapping("/{id}/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update battery health")
    public ResponseEntity<Battery> updateBatteryHealth(
            @PathVariable Long id,
            @RequestParam BigDecimal stateOfHealth) {
        Battery battery = batteryService.updateBatteryHealth(id, stateOfHealth);
        return ResponseEntity.ok(battery);
    }
}