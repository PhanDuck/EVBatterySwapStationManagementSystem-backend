package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.StationInventory;
import com.evbs.BackEndEvBs.model.request.StationInventoryRequest;
import com.evbs.BackEndEvBs.service.StationInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/station-inventory")
@Tag(name = "Station Inventory Management")
public class StationInventoryController {

    @Autowired
    private StationInventoryService stationInventoryService;

    // ==================== ADMIN/STAFF ENDPOINTS ====================

    /**
     * POST /api/station-inventory : Add battery to station (Admin/Staff only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Add battery to station")
    public ResponseEntity<StationInventory> addBatteryToStation(@Valid @RequestBody StationInventoryRequest request) {
        StationInventory inventory = stationInventoryService.addBatteryToStation(request);
        return new ResponseEntity<>(inventory, HttpStatus.CREATED);
    }

    /**
     * GET /api/station-inventory : Get all inventory (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get all inventory")
    public ResponseEntity<List<StationInventory>> getAllInventory() {
        List<StationInventory> inventory = stationInventoryService.getAllInventory();
        return ResponseEntity.ok(inventory);
    }

    /**
     * PATCH /api/station-inventory/{id}/status : Update battery status (Admin/Staff only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Update battery status")
    public ResponseEntity<StationInventory> updateBatteryStatus(
            @PathVariable Long id,
            @RequestParam StationInventory.Status status) {
        StationInventory inventory = stationInventoryService.updateBatteryStatus(id, status);
        return ResponseEntity.ok(inventory);
    }

    /**
     * DELETE /api/station-inventory/{id} : Remove battery from station (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Remove battery from station inventory")
    public ResponseEntity<Void> removeBatteryFromStation(@PathVariable Long id) {
        stationInventoryService.removeBatteryFromStation(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * GET /api/station-inventory/station/{stationId} : Get station inventory (Public)
     */
    @GetMapping("/station/{stationId}")
    @Operation(summary = "Get station inventory")
    public ResponseEntity<List<StationInventory>> getStationInventory(@PathVariable Long stationId) {
        List<StationInventory> inventory = stationInventoryService.getStationInventory(stationId);
        return ResponseEntity.ok(inventory);
    }

    /**
     * GET /api/station-inventory/station/{stationId}/available : Get available batteries (Public)
     */
    @GetMapping("/station/{stationId}/available")
    @Operation(summary = "Get available batteries")
    public ResponseEntity<List<StationInventory>> getAvailableBatteries(@PathVariable Long stationId) {
        List<StationInventory> batteries = stationInventoryService.getAvailableBatteries(stationId);
        return ResponseEntity.ok(batteries);
    }

    /**
     * GET /api/station-inventory/station/{stationId}/capacity : Get station capacity info (Public)
     */
    @GetMapping("/station/{stationId}/capacity")
    @Operation(summary = "Get station capacity information")
    public ResponseEntity<Map<String, Object>> getStationCapacityInfo(@PathVariable Long stationId) {
        Map<String, Object> capacityInfo = stationInventoryService.getStationCapacityInfo(stationId);
        return ResponseEntity.ok(capacityInfo);
    }
}