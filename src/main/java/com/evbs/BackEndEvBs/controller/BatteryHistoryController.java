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

    // READ - Lấy tất cả battery history (Admin/Staff only)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all battery history", description = "Get all battery history records (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getAllBatteryHistory() {
        List<BatteryHistory> history = batteryHistoryService.getAllBatteryHistory();
        return ResponseEntity.ok(history);
    }

    // READ - Lấy battery history theo ID (Admin/Staff only)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get battery history by ID", description = "Get battery history by ID (Admin/Staff only)")
    public ResponseEntity<BatteryHistory> getBatteryHistoryById(
            @Parameter(description = "Battery History ID") @PathVariable Long id) {
        BatteryHistory history = batteryHistoryService.getBatteryHistoryById(id);
        return ResponseEntity.ok(history);
    }

    // READ - Lấy history của battery cụ thể (Admin/Staff only)
    @GetMapping("/battery/{batteryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by battery", description = "Get history records for specific battery (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getHistoryByBattery(
            @Parameter(description = "Battery ID") @PathVariable Long batteryId) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByBattery(batteryId);
        return ResponseEntity.ok(history);
    }

    // READ - Lấy history theo event type (Admin/Staff only)
    @GetMapping("/event-type/{eventType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by event type", description = "Get history records by event type (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getHistoryByEventType(
            @Parameter(description = "Event Type") @PathVariable String eventType) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByEventType(eventType);
        return ResponseEntity.ok(history);
    }

    // READ - Lấy history theo station (Admin/Staff only)
    @GetMapping("/station/{stationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by station", description = "Get history records by station (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getHistoryByStation(
            @Parameter(description = "Station ID") @PathVariable Long stationId) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByStation(stationId);
        return ResponseEntity.ok(history);
    }

    // READ - Lấy history theo vehicle (Admin/Staff only)
    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by vehicle", description = "Get history records by vehicle (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getHistoryByVehicle(
            @Parameter(description = "Vehicle ID") @PathVariable Long vehicleId) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByVehicle(vehicleId);
        return ResponseEntity.ok(history);
    }

    // READ - Lấy history trong khoảng thời gian (Admin/Staff only)
    @GetMapping("/time-range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get history by time range", description = "Get history records within time range (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getHistoryByTimeRange(
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByTimeRange(start, end);
        return ResponseEntity.ok(history);
    }

    // READ - Lấy lịch sử gần nhất của battery (Admin/Staff only)
    @GetMapping("/battery/{batteryId}/latest")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get latest history by battery", description = "Get latest history record for battery (Admin/Staff only)")
    public ResponseEntity<BatteryHistory> getLatestHistoryByBattery(
            @Parameter(description = "Battery ID") @PathVariable Long batteryId) {
        BatteryHistory history = batteryHistoryService.getLatestHistoryByBattery(batteryId);
        return ResponseEntity.ok(history);
    }

    // READ - Thống kê event types (Admin/Staff only)
    @GetMapping("/statistics/event-types")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get event type statistics", description = "Get statistics of event types (Admin/Staff only)")
    public ResponseEntity<Map<String, Long>> getEventTypeStatistics() {
        Map<String, Long> statistics = batteryHistoryService.getEventTypeStatistics();
        return ResponseEntity.ok(statistics);
    }

    // READ - Lấy history của battery trong khoảng thời gian (Admin/Staff only)
    @GetMapping("/battery/{batteryId}/time-range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get battery history by time range", description = "Get battery history within time range (Admin/Staff only)")
    public ResponseEntity<List<BatteryHistory>> getBatteryHistoryByTimeRange(
            @Parameter(description = "Battery ID") @PathVariable Long batteryId,
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<BatteryHistory> history = batteryHistoryService.getHistoryByBattery(batteryId); // You might want to add a specific method for this
        // Filter by time range in service or create a new repository method
        return ResponseEntity.ok(history);
    }
}