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

    /**
     * 🧪 TEST ENDPOINT - CHỈ ĐỂ DEVELOPMENT/TESTING
     * 
     * Bypass MoMo payment để test flow subscription
     * 
     * ⚠️ PRODUCTION: Endpoint này phải DISABLE hoặc chỉ cho ADMIN
     * ⚠️ DEVELOPMENT: Dùng để test subscription flow
     * 
     * @param packageId ID của gói muốn mua
     * @return DriverSubscription được tạo
     */
    @PostMapping("/test-create")
    @PreAuthorize("hasRole('DRIVER')") // Development: DRIVER có thể test
    @Operation(summary = "[TEST] Create subscription without payment (DEVELOPMENT ONLY)", 
               description = "⚠️ BYPASS MoMo payment for testing. MUST BE DISABLED IN PRODUCTION!")
    public ResponseEntity<DriverSubscription> testCreateSubscription(@RequestParam Long packageId) {
        DriverSubscription subscription = driverSubscriptionService.createSubscriptionAfterPayment(packageId);
        return ResponseEntity.ok(subscription);
    }

    /**
     * ❌ DEPRECATED - KHÔNG DÙNG ENDPOINT NÀY!
     * 
     * Endpoint này cho phép tạo subscription mà không cần thanh toán → BỊ LỖI LOGIC!
     * 
     * ✅ ĐÚNG: Driver phải thanh toán qua MoMo:
     * 1. Driver chọn gói → Gọi POST /api/payment/momo/create?packageId=1
     * 2. System trả về paymentUrl → Driver redirect đến MoMo
     * 3. Driver thanh toán thành công → MoMo callback /api/payment/momo-return
     * 4. System tự động tạo subscription ACTIVE → Driver có thể swap miễn phí
     * 
     * ⚠️ Endpoint này CHỈ để admin test, KHÔNG để driver dùng!
     */
    @Deprecated
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // ❌ Đổi từ DRIVER → ADMIN (chỉ admin test được)
    @Operation(summary = "[DEPRECATED] Create subscription without payment (ADMIN TEST ONLY)", 
               description = "⚠️ DO NOT USE! Use POST /api/payment/momo/create instead. This endpoint bypasses payment and is for ADMIN TESTING ONLY.")
    public ResponseEntity<Map<String, String>> createSubscription(@Valid @RequestBody DriverSubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
            "error", "DEPRECATED",
            "message", "❌ Endpoint này không được dùng! Driver phải thanh toán qua MoMo.",
            "correctEndpoint", "POST /api/payment/momo/create?packageId={packageId}",
            "reason", "Tạo subscription phải có thanh toán. Dùng endpoint này sẽ tạo gói miễn phí (BUG!)"
        ));
    }

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