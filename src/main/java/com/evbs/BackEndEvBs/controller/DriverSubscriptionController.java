package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.model.request.DriverSubscriptionRequest;
import com.evbs.BackEndEvBs.service.DriverSubscriptionService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/driver-subscription")
@SecurityRequirement(name = "api")
@Tag(name = "Driver Subscription Management", description = "APIs for managing driver subscriptions")
public class DriverSubscriptionController {

    @Autowired
    private DriverSubscriptionService driverSubscriptionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all subscriptions", description = "Get all subscriptions (Admin only)")
    public ResponseEntity<List<DriverSubscription>> getAllSubscriptions() {
        List<DriverSubscription> subscriptions = driverSubscriptionService.getAllSubscriptions();
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/my-subscriptions")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get my subscriptions", description = "Get subscriptions for current driver")
    public ResponseEntity<List<DriverSubscription>> getMySubscriptions() {
        List<DriverSubscription> subscriptions = driverSubscriptionService.getMySubscriptions();
        return ResponseEntity.ok(subscriptions);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update subscription", description = "Update subscription information (Admin only)")
    public ResponseEntity<DriverSubscription> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody DriverSubscriptionRequest request) {
        DriverSubscription subscription = driverSubscriptionService.updateSubscription(id, request);
        return ResponseEntity.ok(subscription);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete subscription", description = "Delete subscription (Admin only)")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        driverSubscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }
}