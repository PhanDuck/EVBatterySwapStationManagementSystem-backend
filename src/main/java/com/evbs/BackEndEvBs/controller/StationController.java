package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.model.request.CreateStationRequest;
import com.evbs.BackEndEvBs.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@SecurityRequirement(name = "api")
@RequestMapping("/api/station")
public class StationController {

    @Autowired
    StationService stationService;

    // 🔹 Lấy tất cả trạm (Admin/Staff only)
    @GetMapping
//    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all stations")
    public ResponseEntity<List<Station>> getAllStations() {
        List<Station> stations = stationService.getAllStations();
        return ResponseEntity.ok(stations);
    }

    // 🔹 Lấy trạm theo ID (Admin/Staff only)
    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get station by ID")
    public ResponseEntity<Station> getStationById(@PathVariable Long id) {
        Station station = stationService.getStationById(id);
        return ResponseEntity.ok(station);
    }

    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new station")
    public ResponseEntity<CreateStationRequest> createStation(@Valid @RequestBody CreateStationRequest request) {
        CreateStationRequest created = stationService.createStation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
