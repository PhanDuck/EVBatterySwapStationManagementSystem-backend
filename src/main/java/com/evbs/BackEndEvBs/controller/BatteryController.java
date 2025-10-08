package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.model.request.BatteryRequest;
import com.evbs.BackEndEvBs.service.BatteryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Battery Management", description = "APIs for managing batteries")
public class BatteryController {

    @Autowired
    private BatteryService batteryService;

    /**
     * POST /api/battery : Create new battery (Admin/Staff only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Create new battery", description = "Create a new battery (Admin/Staff only)")
    public ResponseEntity<Battery> createBattery(@Valid @RequestBody BatteryRequest request) {
        Battery battery = batteryService.createBattery(request);
        return new ResponseEntity<>(battery, HttpStatus.CREATED);
    }

    /**
     * GET /api/battery : Get all batteries (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all batteries", description = "Get list of all batteries (Admin/Staff only)")
    public ResponseEntity<List<Battery>> getAllBatteries() {
        List<Battery> batteries = batteryService.getAllBatteries();
        return ResponseEntity.ok(batteries);
    }

    /**
     * GET /api/battery/{id} : Get battery by ID (Admin/Staff only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get battery by ID", description = "Get battery details by ID (Admin/Staff only)")
    public ResponseEntity<Battery> getBatteryById(
            @Parameter(description = "Battery ID") @PathVariable Long id) {
        Battery battery = batteryService.getBatteryById(id);
        return ResponseEntity.ok(battery);
    }

    /**
     * GET /api/battery/station/{stationId} : Get batteries by station (Admin/Staff only)
     */
    @GetMapping("/station/{stationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get batteries by station", description = "Get batteries by station ID (Admin/Staff only)")
    public ResponseEntity<List<Battery>> getBatteriesByStation(
            @Parameter(description = "Station ID") @PathVariable Long stationId) {
        List<Battery> batteries = batteryService.getBatteriesByStation(stationId);
        return ResponseEntity.ok(batteries);
    }

    /**
     * GET /api/battery/station/{stationId}/available : Get available batteries at station (Public)
     */
    @GetMapping("/station/{stationId}/available")
    @Operation(summary = "Get available batteries at station", description = "Get available batteries at specific station (Public)")
    public ResponseEntity<List<Battery>> getAvailableBatteriesAtStation(
            @Parameter(description = "Station ID") @PathVariable Long stationId) {
        List<Battery> batteries = batteryService.getAvailableBatteriesAtStation(stationId);
        return ResponseEntity.ok(batteries);
    }

    /**
     * GET /api/battery/status/{status} : Get batteries by status (Admin/Staff only)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get batteries by status", description = "Get batteries by status (Admin/Staff only)")
    public ResponseEntity<List<Battery>> getBatteriesByStatus(
            @Parameter(description = "Battery status") @PathVariable String status) {
        List<Battery> batteries = batteryService.getBatteriesByStatus(status);
        return ResponseEntity.ok(batteries);
    }

    /**
     * GET /api/battery/station/{stationId}/available/count : Count available batteries at station (Public)
     */
    @GetMapping("/station/{stationId}/available/count")
    @Operation(summary = "Count available batteries at station", description = "Count available batteries at specific station (Public)")
    public ResponseEntity<Long> countAvailableBatteriesAtStation(
            @Parameter(description = "Station ID") @PathVariable Long stationId) {
        long count = batteryService.countAvailableBatteriesAtStation(stationId);
        return ResponseEntity.ok(count);
    }

    /**
     * PUT /api/battery/{id} : Update battery (Admin/Staff only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update battery", description = "Update battery information (Admin/Staff only)")
    public ResponseEntity<Battery> updateBattery(
            @Parameter(description = "Battery ID") @PathVariable Long id,
            @Valid @RequestBody BatteryRequest request) {
        Battery battery = batteryService.updateBattery(id, request);
        return ResponseEntity.ok(battery);
    }

    /**
     * PATCH /api/battery/{id}/status : Update battery status (Admin/Staff only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update battery status", description = "Update only battery status (Admin/Staff only)")
    public ResponseEntity<Battery> updateBatteryStatus(
            @Parameter(description = "Battery ID") @PathVariable Long id,
            @RequestParam String status) {
        Battery battery = batteryService.updateBatteryStatus(id, status);
        return ResponseEntity.ok(battery);
    }

    /**
     * PATCH /api/battery/{id}/soh : Update battery state of health (Admin/Staff only)
     */
    @PatchMapping("/{id}/soh")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update battery state of health", description = "Update battery state of health (Admin/Staff only)")
    public ResponseEntity<Battery> updateBatterySOH(
            @Parameter(description = "Battery ID") @PathVariable Long id,
            @RequestParam BigDecimal stateOfHealth) {
        Battery battery = batteryService.updateBatterySOH(id, stateOfHealth);
        return ResponseEntity.ok(battery);
    }

    /**
     * DELETE /api/battery/{id} : Delete battery (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete battery", description = "Delete battery (Admin only)")
    public ResponseEntity<Void> deleteBattery(
            @Parameter(description = "Battery ID") @PathVariable Long id) {
        batteryService.deleteBattery(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/battery/health/good : Get batteries with good health (Admin/Staff only)
     */
    @GetMapping("/health/good")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get batteries with good health", description = "Get batteries with state of health >= min value (Admin/Staff only)")
    public ResponseEntity<List<Battery>> getBatteriesWithGoodHealth(
            @RequestParam(defaultValue = "80") BigDecimal minSoh) {
        List<Battery> batteries = batteryService.getBatteriesWithGoodHealth(minSoh);
        return ResponseEntity.ok(batteries);
    }
}