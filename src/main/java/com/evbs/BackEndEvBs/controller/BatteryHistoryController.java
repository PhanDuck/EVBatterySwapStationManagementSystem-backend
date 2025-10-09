package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.BatteryHistory;
import com.evbs.BackEndEvBs.service.BatteryHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/battery-history")
@SecurityRequirement(name = "api")
@Tag(name = "Battery History Management")
public class BatteryHistoryController {

    @Autowired
    private BatteryHistoryService batteryHistoryService;

    // ==================== READ ONLY ENDPOINTS ====================

    /**
     * GET /api/battery-history : Get all battery history (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all battery history")
    public ResponseEntity<List<BatteryHistory>> getAllBatteryHistory() {
        List<BatteryHistory> history = batteryHistoryService.getAllBatteryHistory();
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/my-history : Get my battery history (Staff/Admin only)
     */
    @GetMapping("/my-history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get my battery history")
    public ResponseEntity<List<BatteryHistory>> getMyBatteryHistory() {
        List<BatteryHistory> history = batteryHistoryService.getMyBatteryHistory();
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/battery/{batteryId} : Get history by battery (Admin/Staff only)
     */
    @GetMapping("/battery/{batteryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by battery")
    public ResponseEntity<List<BatteryHistory>> getBatteryHistoryByBattery(@PathVariable Long batteryId) {
        List<BatteryHistory> history = batteryHistoryService.getBatteryHistoryByBattery(batteryId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/event-type/{eventType} : Get history by event type (Admin/Staff only)
     */
    @GetMapping("/event-type/{eventType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by event type")
    public ResponseEntity<List<BatteryHistory>> getBatteryHistoryByEventType(@PathVariable String eventType) {
        List<BatteryHistory> history = batteryHistoryService.getBatteryHistoryByEventType(eventType);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/station/{stationId} : Get history by station (Admin/Staff only)
     */
    @GetMapping("/station/{stationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by station")
    public ResponseEntity<List<BatteryHistory>> getBatteryHistoryByStation(@PathVariable Long stationId) {
        List<BatteryHistory> history = batteryHistoryService.getBatteryHistoryByStation(stationId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/vehicle/{vehicleId} : Get history by vehicle (Admin/Staff only)
     */
    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by vehicle")
    public ResponseEntity<List<BatteryHistory>> getBatteryHistoryByVehicle(@PathVariable Long vehicleId) {
        List<BatteryHistory> history = batteryHistoryService.getBatteryHistoryByVehicle(vehicleId);
        return ResponseEntity.ok(history);
    }
}