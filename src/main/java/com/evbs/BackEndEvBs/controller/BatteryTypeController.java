package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.model.request.BatteryTypeRequest;
import com.evbs.BackEndEvBs.service.BatteryTypeService;
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
@RequestMapping("/api/battery-type")
@SecurityRequirement(name = "api")
@Tag(name = "Battery Type Management")
public class BatteryTypeController {

    @Autowired
    private BatteryTypeService batteryTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new battery type")
    public ResponseEntity<BatteryType> createBatteryType(@Valid @RequestBody BatteryTypeRequest request) {
        BatteryType batteryType = batteryTypeService.createBatteryType(request);
        return new ResponseEntity<>(batteryType, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all battery types")
    public ResponseEntity<List<BatteryType>> getAllBatteryTypes() {
        List<BatteryType> batteryTypes = batteryTypeService.getAllBatteryTypes();
        return ResponseEntity.ok(batteryTypes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get battery type by ID")
    public ResponseEntity<BatteryType> getBatteryTypeById(@PathVariable Long id) {
        BatteryType batteryType = batteryTypeService.getBatteryTypeById(id);
        return ResponseEntity.ok(batteryType);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update battery type")
    public ResponseEntity<BatteryType> updateBatteryType(
            @PathVariable Long id,
            @Valid @RequestBody BatteryTypeRequest request) {
        BatteryType batteryType = batteryTypeService.updateBatteryType(id, request);
        return ResponseEntity.ok(batteryType);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete battery type")
    public ResponseEntity<Void> deleteBatteryType(@PathVariable Long id) {
        batteryTypeService.deleteBatteryType(id);
        return ResponseEntity.noContent().build();
    }
}