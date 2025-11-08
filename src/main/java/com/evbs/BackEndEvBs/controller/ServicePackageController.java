package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.model.request.ServicePackageRequest;
import com.evbs.BackEndEvBs.model.request.ServicePackageUpdateRequest;
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
@Tag(name = "Service Package Management", description = "APIs for managing service packages")
public class ServicePackageController {

    @Autowired
    private ServicePackageService servicePackageService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "api")
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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Update service package", description = "Update service package information (Admin only)")
    public ResponseEntity<ServicePackage> updateServicePackage(
            @Parameter(description = "Service Package ID") @PathVariable Long id,
            @Valid @RequestBody ServicePackageUpdateRequest request) {
        ServicePackage servicePackage = servicePackageService.updateServicePackage(id, request);
        return ResponseEntity.ok(servicePackage);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Delete service package", description = "Delete service package (Admin only)")
    public ResponseEntity<Void> deleteServicePackage(
            @Parameter(description = "Service Package ID") @PathVariable Long id) {
        servicePackageService.deleteServicePackage(id);
        return ResponseEntity.noContent().build();
    }
}