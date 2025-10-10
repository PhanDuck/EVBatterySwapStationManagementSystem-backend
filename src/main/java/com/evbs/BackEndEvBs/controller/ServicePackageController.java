package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.model.request.ServicePackageRequest;
import com.evbs.BackEndEvBs.service.ServicePackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/service-package")
@SecurityRequirement(name = "api")
@Tag(name = "Service Package Management", description = "APIs for managing service packages")
public class ServicePackageController {

    @Autowired
    private ServicePackageService servicePackageService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create service package", description = "Create new service package (Admin only)")
    public ResponseEntity<ServicePackage> createServicePackage(@Valid @RequestBody ServicePackageRequest request) {
        ServicePackage servicePackage = servicePackageService.createServicePackage(request);
        return new ResponseEntity<>(servicePackage, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all service packages", description = "Get list of all service packages (Public)")
    public ResponseEntity<List<ServicePackage>> getAllServicePackages() {
        List<ServicePackage> packages = servicePackageService.getAllServicePackages();
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service package by ID", description = "Get service package details by ID (Public)")
    public ResponseEntity<ServicePackage> getServicePackageById(
            @Parameter(description = "Service Package ID") @PathVariable Long id) {
        ServicePackage servicePackage = servicePackageService.getServicePackageById(id);
        return ResponseEntity.ok(servicePackage);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update service package", description = "Update service package information (Admin only)")
    public ResponseEntity<ServicePackage> updateServicePackage(
            @Parameter(description = "Service Package ID") @PathVariable Long id,
            @Valid @RequestBody ServicePackageRequest request) {
        ServicePackage servicePackage = servicePackageService.updateServicePackage(id, request);
        return ResponseEntity.ok(servicePackage);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete service package", description = "Delete service package (Admin only)")
    public ResponseEntity<Void> deleteServicePackage(
            @Parameter(description = "Service Package ID") @PathVariable Long id) {
        servicePackageService.deleteServicePackage(id);
        return ResponseEntity.noContent().build();
    }
}