package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.model.request.StationRequest;
import com.evbs.BackEndEvBs.model.request.StationUpdateRequest;
import com.evbs.BackEndEvBs.service.BatteryService;
import com.evbs.BackEndEvBs.service.StationService;
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

@RestController
@RequestMapping("/api/station")
@Tag(name = "Station Management")
public class StationController {

    @Autowired
    private StationService stationService;

    @Autowired
    private BatteryService batteryService;

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * GET /api/station : Get all stations (Public)
     */
    @GetMapping
    @Operation(summary = "Get all stations")
    public ResponseEntity<List<Station>> getAllStations() {
        List<Station> stations = stationService.getAllStations();
        return ResponseEntity.ok(stations);
    }

    /**
     * GET /api/station/{id} : Get station by ID (Public)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get station by ID")
    public ResponseEntity<Station> getStationById(@PathVariable Long id) {
        Station station = stationService.getStationById(id);
        return ResponseEntity.ok(station);
    }

    /**
     * GET /api/station/{id}/batteries : Get all batteries in station (Public)
     */
    @GetMapping("/{id}/batteries")
    @Operation(summary = "Get all batteries in station")
    public ResponseEntity<List<Battery>> getBatteriesByStation(@PathVariable Long id) {
        List<Battery> batteries = batteryService.getBatteriesByStation(id);
        return ResponseEntity.ok(batteries);
    }

    // ==================== ADMIN/STAFF ENDPOINTS ====================

    /**
     * POST /api/station : Create new station (Admin/Staff only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Create new station")
    public ResponseEntity<Station> createStation(@Valid @RequestBody StationRequest request) {
        Station station = stationService.createStation(request);
        return new ResponseEntity<>(station, HttpStatus.CREATED);
    }

    /**
     * PUT /api/station/{id} : Update station (Admin/Staff only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Update station")
    public ResponseEntity<Station> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody StationUpdateRequest request) {
        Station station = stationService.updateStation(id, request);
        return ResponseEntity.ok(station);
    }

    /**
     * DELETE /api/station/{id} : Delete station (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Delete station")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/station/{id}/status : Update station status (Admin/Staff only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Update station status")
    public ResponseEntity<Station> updateStationStatus(
            @PathVariable Long id,
            @RequestParam Station.Status status) {
        Station station = stationService.updateStationStatus(id, status);
        return ResponseEntity.ok(station);
    }
}
