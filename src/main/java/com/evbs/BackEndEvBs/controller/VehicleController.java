package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.model.request.*;
import com.evbs.BackEndEvBs.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@SecurityRequirement(name = "api")
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Autowired
    VehicleService vehicleService;

    /**
     * POST /api/vehicle : Creates a new vehicle (Driver)
     * Nhận form-data với file upload
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new vehicle with registration image upload (Driver)")
    public ResponseEntity<Vehicle> createVehicle(@Valid @ModelAttribute VehicleCreateRequest request) {

        // Tạo VehicleRequest từ VehicleCreateRequest
        VehicleRequest vehicleRequest = new VehicleRequest();
        vehicleRequest.setVin(request.getVin());
        vehicleRequest.setPlateNumber(request.getPlateNumber());
        vehicleRequest.setModel(request.getModel());
        vehicleRequest.setBatteryTypeId(request.getBatteryTypeId());

        Vehicle newVehicle = vehicleService.createVehicle(vehicleRequest, request.getRegistrationImage());
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
     * Admin/Staff được update: vin, plateNumber, model, driverId, batteryTypeId, status
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update vehicle (Admin/Staff) - all fields except image")
    public ResponseEntity<Vehicle> updateVehicle(
            @PathVariable Long id,
            @Valid @ModelAttribute VehicleUpdateRequest request) {
        
        Vehicle updatedVehicle = vehicleService.updateVehicle(id, request, null);
        return ResponseEntity.ok(updatedVehicle);
    }

    /**
     * DELETE /api/vehicle/{id} : Delete vehicle by ID (Admin/Staff only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete vehicle by ID (Admin/Staff only)")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/vehicle/{id}/approve : Approve vehicle with battery assignment (Admin/Staff only)
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve vehicle and assign battery (Admin/Staff only)")
    public ResponseEntity<Vehicle> approveVehicle(
            @PathVariable Long id,
            @RequestBody VehicleApproveRequest request) {
        Vehicle approvedVehicle = vehicleService.approveVehicle(id, request);
        return ResponseEntity.ok(approvedVehicle);
    }

    /**
     * PUT /api/vehicle/{id}/reject : Reject vehicle (Admin/Staff only)
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject vehicle with optional reason (Admin/Staff only)")
    public ResponseEntity<Vehicle> rejectVehicle(
            @PathVariable Long id,
            @RequestBody(required = false) VehicleRejectRequest request) {
        Vehicle rejectedVehicle = vehicleService.rejectVehicle(id, request);
        return ResponseEntity.ok(rejectedVehicle);
    }
}