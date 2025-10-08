package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.BatteryHistory;
import com.evbs.BackEndEvBs.service.BatteryHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/battery-history")
@SecurityRequirement(name = "api")
@Tag(name = "Battery History", description = "APIs for battery history (Read Only)")
public class BatteryHistoryController {

    @Autowired
    private BatteryHistoryService batteryHistoryService;

    /**
     * GET /api/battery-history : Get all battery history (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all battery history", description = "Get all battery history records (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getAllBatteryHistory() {
        List<BatteryHistory> history = batteryHistoryService.getAllBatteryHistory();
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/{id} : Get battery history by ID (Admin/Staff only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get battery history by ID", description = "Get battery history by ID (Admin/Staff only)")
    public ResponseEntity<BatteryHistory> getBatteryHistoryById(
            @Parameter(description = "Battery History ID") @PathVariable Long id) {
        BatteryHistory history = batteryHistoryService.getBatteryHistoryById(id);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/battery/{batteryId} : Get history by battery (Admin/Staff only)
     */
    @GetMapping("/battery/{batteryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by battery", description = "Get history records for specific battery (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getHistoryByBattery(
            @Parameter(description = "Battery ID") @PathVariable Long batteryId) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByBattery(batteryId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/event-type/{eventType} : Get history by event type (Admin/Staff only)
     */
    @GetMapping("/event-type/{eventType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by event type", description = "Get history records by event type (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getHistoryByEventType(
            @Parameter(description = "Event Type") @PathVariable String eventType) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByEventType(eventType);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/station/{stationId} : Get history by station (Admin/Staff only)
     */
    @GetMapping("/station/{stationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by station", description = "Get history records by station (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getHistoryByStation(
            @Parameter(description = "Station ID") @PathVariable Long stationId) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByStation(stationId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/vehicle/{vehicleId} : Get history by vehicle (Admin/Staff only)
     */
    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by vehicle", description = "Get history records by vehicle (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getHistoryByVehicle(
            @Parameter(description = "Vehicle ID") @PathVariable Long vehicleId) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByVehicle(vehicleId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/time-range : Get history by time range (Admin/Staff only)
     */
    @GetMapping("/time-range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by time range", description = "Get history records within time range (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getHistoryByTimeRange(
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByTimeRange(start, end);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/battery/{batteryId}/latest : Get latest history by battery (Admin/Staff only)
     */
    @GetMapping("/battery/{batteryId}/latest")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get latest history by battery", description = "Get latest history record for battery (Admin/Staff only)")
    public ResponseEntity<BatteryHistory> getLatestHistoryByBattery(
            @Parameter(description = "Battery ID") @PathVariable Long batteryId) {
        BatteryHistory history = batteryHistoryService.getLatestHistoryByBattery(batteryId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/battery-history/statistics/event-types : Get event type statistics (Admin/Staff only)
     */
    @GetMapping("/statistics/event-types")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get event type statistics", description = "Get statistics of event types (Admin/Staff only)")
    public ResponseEntity<Map<String, Long>> getEventTypeStatistics() {
        Map<String, Long> statistics = batteryHistoryService.getEventTypeStatistics();
        return ResponseEntity.ok(statistics);
    }
}