package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.model.request.StationRequest;
import com.evbs.BackEndEvBs.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/station")
@SecurityRequirement(name = "api")
@Tag(name = "Station Management", description = "APIs for managing stations")
public class StationController {

    @Autowired
    private StationService stationService;

    // ==================== PUBLIC READ ENDPOINTS ====================

    // READ - Lấy tất cả stations (Public - Anyone can view)
    @GetMapping
    @Operation(summary = "Get all stations", description = "Get list of all stations (Public)")
    public ResponseEntity<List<Station>> getAllStations() {
        List<Station> stations = stationService.getAllStations();
        return ResponseEntity.ok(stations);
    }

    // READ - Lấy station theo ID (Public - Anyone can view)
    @GetMapping("/{id}")
    @Operation(summary = "Get station by ID", description = "Get station details by ID (Public)")
    public ResponseEntity<Station> getStationById(
            @Parameter(description = "Station ID") @PathVariable Long id) {
        Station station = stationService.getStationById(id);
        return ResponseEntity.ok(station);
    }

    // READ - Lấy active stations (Public)
    @GetMapping("/active")
    @Operation(summary = "Get active stations", description = "Get list of active stations (Public)")
    public ResponseEntity<List<Station>> getActiveStations() {
        List<Station> stations = stationService.getActiveStations();
        return ResponseEntity.ok(stations);
    }

    // READ - Tìm stations theo name (Public)
    @GetMapping("/search/name")
    @Operation(summary = "Search stations by name", description = "Search stations by name (Public)")
    public ResponseEntity<List<Station>> searchStationsByName(
            @RequestParam String name) {
        List<Station> stations = stationService.searchStationsByName(name);
        return ResponseEntity.ok(stations);
    }

    // READ - Tìm stations theo location (Public)
    @GetMapping("/search/location")
    @Operation(summary = "Search stations by location", description = "Search stations by location (Public)")
    public ResponseEntity<List<Station>> searchStationsByLocation(
            @RequestParam String location) {
        List<Station> stations = stationService.searchStationsByLocation(location);
        return ResponseEntity.ok(stations);
    }

    // READ - Lấy stations có available batteries (Public)
    @GetMapping("/available-batteries")
    @Operation(summary = "Get stations with available batteries", description = "Get stations with available batteries (Public)")
    public ResponseEntity<List<Station>> getStationsWithAvailableBatteries() {
        List<Station> stations = stationService.getStationsWithAvailableBatteries();
        return ResponseEntity.ok(stations);
    }

    // READ - Lấy stations theo capacity range (Public)
    @GetMapping("/capacity-range")
    @Operation(summary = "Get stations by capacity range", description = "Get stations by capacity range (Public)")
    public ResponseEntity<List<Station>> getStationsByCapacityRange(
            @RequestParam Integer minCapacity,
            @RequestParam Integer maxCapacity) {
        List<Station> stations = stationService.getStationsByCapacityRange(minCapacity, maxCapacity);
        return ResponseEntity.ok(stations);
    }

    // READ - Lấy stations theo status (Public)
    @GetMapping("/status/{status}")
    @Operation(summary = "Get stations by status", description = "Get stations by status (Public)")
    public ResponseEntity<List<Station>> getStationsByStatus(
            @Parameter(description = "Station status") @PathVariable String status) {
        List<Station> stations = stationService.getStationsByStatus(status);
        return ResponseEntity.ok(stations);
    }

    // ==================== ADMIN/STAFF ENDPOINTS ====================

    // CREATE - Tạo station mới (Admin/Staff only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Create new station", description = "Create a new station (Admin/Staff only)")
    public ResponseEntity<Station> createStation(@Valid @RequestBody StationRequest request) {
        Station station = stationService.createStation(request);
        return ResponseEntity.ok(station);
    }

    // UPDATE - Cập nhật station (Admin/Staff only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update station", description = "Update station information (Admin/Staff only)")
    public ResponseEntity<Station> updateStation(
            @Parameter(description = "Station ID") @PathVariable Long id,
            @Valid @RequestBody StationRequest request) {
        Station station = stationService.updateStation(id, request);
        return ResponseEntity.ok(station);
    }

    // UPDATE - Chỉ cập nhật status (Admin/Staff only)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update station status", description = "Update only station status (Admin/Staff only)")
    public ResponseEntity<Station> updateStationStatus(
            @Parameter(description = "Station ID") @PathVariable Long id,
            @RequestParam String status) {
        Station station = stationService.updateStationStatus(id, status);
        return ResponseEntity.ok(station);
    }

    // UPDATE - Chỉ cập nhật capacity (Admin/Staff only)
    @PatchMapping("/{id}/capacity")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update station capacity", description = "Update only station capacity (Admin/Staff only)")
    public ResponseEntity<Station> updateStationCapacity(
            @Parameter(description = "Station ID") @PathVariable Long id,
            @RequestParam Integer capacity) {
        Station station = stationService.updateStationCapacity(id, capacity);
        return ResponseEntity.ok(station);
    }

    // DELETE - Xóa station (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete station", description = "Delete station (Admin only)")
    public ResponseEntity<?> deleteStation(
            @Parameter(description = "Station ID") @PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.ok().build();
    }

    // SOFT DELETE - Deactivate station (Admin/Staff only)
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Deactivate station", description = "Deactivate station (Admin/Staff only)")
    public ResponseEntity<Station> deactivateStation(
            @Parameter(description = "Station ID") @PathVariable Long id) {
        Station station = stationService.deactivateStation(id);
        return ResponseEntity.ok(station);
    }

    // ==================== UTILITY ENDPOINTS ====================

    // READ - Đếm số stations theo status (Admin/Staff only)
    @GetMapping("/count/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Count stations by status", description = "Count stations by status (Admin/Staff only)")
    public ResponseEntity<Long> countStationsByStatus(
            @Parameter(description = "Station status") @PathVariable String status) {
        long count = stationService.countStationsByStatus(status);
        return ResponseEntity.ok(count);
    }

    // READ - Lấy station statistics (Admin/Staff only)
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get station statistics", description = "Get station statistics (Admin/Staff only)")
    public ResponseEntity<StationService.StationStatistics> getStationStatistics() {
        StationService.StationStatistics statistics = stationService.getStationStatistics();
        return ResponseEntity.ok(statistics);
    }

    // READ - Kiểm tra station có tồn tại không (Public)
    @GetMapping("/{id}/exists")
    @Operation(summary = "Check if station exists", description = "Check if station exists by ID (Public)")
    public ResponseEntity<Boolean> stationExists(
            @Parameter(description = "Station ID") @PathVariable Long id) {
        boolean exists = stationService.stationExists(id);
        return ResponseEntity.ok(exists);
    }
}