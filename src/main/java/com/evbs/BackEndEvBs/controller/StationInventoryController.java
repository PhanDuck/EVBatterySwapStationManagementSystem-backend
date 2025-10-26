package com.evbs.BackEndEvBs.controller;

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
@SecurityRequirement(name = "api")
@Tag(name = "Station Inventory Management")
public class StationInventoryController {

    @Autowired
    private StationInventoryService stationInventoryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all batteries in warehouse")
    public ResponseEntity<Map<String, Object>> getAllBatteriesInWarehouse() {
        return ResponseEntity.ok(stationInventoryService.getAllBatteriesInWarehouseWithDetails());
    }

    @GetMapping("/warehouse/needs-maintenance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get batteries needing maintenance IN WAREHOUSE")
    public ResponseEntity<Map<String, Object>> getBatteriesNeedingMaintenanceInWarehouse() {
        return ResponseEntity.ok(stationInventoryService.getBatteriesNeedingMaintenanceInWarehouse());
    }

    @GetMapping("/available-by-type/{batteryTypeId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get available batteries in warehouse by battery type")
    public ResponseEntity<Map<String, Object>> getAvailableBatteriesByType(@PathVariable Long batteryTypeId) {
        return ResponseEntity.ok(stationInventoryService.getAvailableBatteriesByType(batteryTypeId));
    }

    @PostMapping("/move-to-station")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Move battery from warehouse to station")
    public ResponseEntity<Map<String, Object>> moveBatteryToStation(
            @RequestParam Long batteryId,
            @RequestParam Long stationId,
            @RequestParam Long batteryTypeId) {
        return ResponseEntity.ok(stationInventoryService.moveBatteryToStation(batteryId, stationId, batteryTypeId));
    }

    @PostMapping("/move-to-warehouse")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Move maintenance battery from station to warehouse")
    public ResponseEntity<Map<String, Object>> moveBatteryToWarehouse(
            @RequestParam Long batteryId,
            @RequestParam Long stationId) {
        return ResponseEntity.ok(stationInventoryService.moveBatteryToWarehouse(batteryId, stationId));
    }



    @PatchMapping("/{batteryId}/complete-maintenance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete battery maintenance and update SOH")
    public ResponseEntity<Map<String, Object>> completeMaintenance(
            @PathVariable Long batteryId,
            @RequestParam Double newSOH) {
        return ResponseEntity.ok(stationInventoryService.completeMaintenance(batteryId, newSOH));
    }
}