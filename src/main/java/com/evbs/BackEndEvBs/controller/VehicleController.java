package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.model.request.VehicleRequest;
import com.evbs.BackEndEvBs.model.request.VehicleUpdateRequest;
import com.evbs.BackEndEvBs.service.VehicleService;
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
@SecurityRequirement(name = "api")
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Autowired
    VehicleService vehicleService;

    /**
     * POST /api/vehicle : Creates a new vehicle (Driver)
     */
    @PostMapping
    @Operation(summary = "Create a new vehicle (Driver)")
    public ResponseEntity<Vehicle> createVehicle(@Valid @RequestBody VehicleRequest vehicleRequest) {
        Vehicle newVehicle = vehicleService.createVehicle(vehicleRequest);
        return new ResponseEntity<>(newVehicle, HttpStatus.CREATED);
    }

    /**
     * GET /api/vehicle/my-vehicles : Get my vehicles (Driver only)
     */
    @GetMapping("/my-vehicles")
    @Operation(summary = "Get my vehicles (Driver only)")
    public ResponseEntity<List<Vehicle>> getMyVehicles() {
        List<Vehicle> myVehicles = vehicleService.getMyVehicles();
        return ResponseEntity.ok(myVehicles);
    }

    /**
     * PUT /api/vehicle/my-vehicles/{id} : Update vehicle's info by ID (Driver)
     */
    @PutMapping("/my-vehicles/{id}")
    @Operation(summary = "Update vehicle's info by ID (Driver)")
    public ResponseEntity<Vehicle> updateMyVehicle(@PathVariable Long id, @RequestBody VehicleUpdateRequest vehicleRequest) {
        Vehicle updatedVehicle = vehicleService.updateMyVehicle(id, vehicleRequest);
        return ResponseEntity.ok(updatedVehicle);
    }

    /**
     * GET /api/vehicle : Get all vehicles (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all vehicles")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(vehicles);
    }


    /**
     * PUT /api/vehicle/{id} : Update vehicle's info by ID (Admin/Staff only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update vehicle's info by ID (Admin/Staff only)")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id, @Valid @RequestBody VehicleUpdateRequest vehicleRequest) {
        Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicleRequest);
        return ResponseEntity.ok(updatedVehicle);
    }

    /**
     * DELETE /api/vehicle/{id} : Delete vehicle by ID (Admin/Staff only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Delete vehicle by ID (Admin/Staff only)")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}