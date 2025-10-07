package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping
    @Operation(summary = "Create a new vehicle (Driver)")
    public ResponseEntity createVehicle(@Valid @RequestBody Vehicle vehicle) {
        Vehicle newVehicle = vehicleService.createVehicle(vehicle);
        return ResponseEntity.ok(newVehicle);
    }

//    //Lấy danh sách xe của tôi (Driver)
//    @GetMapping("/my-vehicles")
//    public ResponseEntity<List<Vehicle>> getMyVehicles() {
//        List<Vehicle> vehicles = vehicleService.getMyVehicles();
//        return ResponseEntity.ok(vehicles);
//    }
//
//    //Lấy xe của tôi theo ID (Driver)
//    @GetMapping("/my-vehicles/{id}")
//    public ResponseEntity<Vehicle> getMyVehicleById(@PathVariable Long id) {
//        Vehicle vehicle = vehicleService.getMyVehicleById(id);
//        return ResponseEntity.ok(vehicle);
//    }

    // Cập nhật thông tin không quan trọng (Driver)
    @PutMapping("/my-vehicles/{id}")
    @Operation(summary = "Update vehicle's info by ID (Driver)")
    public ResponseEntity<Vehicle> updateMyVehicle(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        Vehicle updatedVehicle = vehicleService.updateMyVehicle(id, vehicle);
        return ResponseEntity.ok(updatedVehicle);
    }

    //Lấy tất cả xe (Admin/Staff only)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all vehicles")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(vehicles);
    }

    //Lấy xe theo user ID (Admin/Staff only)
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get vehicles by user ID")
    public ResponseEntity<List<Vehicle>> getVehiclesByUserId(@PathVariable Long userId) {
        List<Vehicle> vehicles = vehicleService.getVehiclesByUserId(userId);
        return ResponseEntity.ok(vehicles);
    }

    //Cập nhật đầy đủ thông tin xe (Admin/Staff only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update vehicle's info by ID (Admin/Staff only)")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id, @Valid @RequestBody Vehicle vehicle) {
        Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicle);
        return ResponseEntity.ok(updatedVehicle);
    }

    // Xóa xe (Admin/Staff only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Delete vehicle by ID (Admin/Staff only)")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok().build();
    }
}
