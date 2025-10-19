package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Payment;
import com.evbs.BackEndEvBs.model.request.PaymentRequest;
import com.evbs.BackEndEvBs.service.MoMoService;
import com.evbs.BackEndEvBs.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@SecurityRequirement(name = "api")
@Tag(name = "Payment Management", description = "APIs for payment processing")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MoMoService moMoService;

    // ==================== STAFF/ADMIN ENDPOINTS ====================

    /**
     * ❌ DEPRECATED - Swap không cần thanh toán riêng
     * Thanh toán chỉ khi MUA GÓI (DriverSubscription)
     */
    @Deprecated
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "[DEPRECATED] Use POST /api/driver-subscription instead", 
               description = "Swap không cần thanh toán. Thanh toán chỉ khi mua gói dịch vụ.")
    public ResponseEntity<Map<String, String>> createPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "❌ API này không còn dùng nữa",
            "message", "Swap MIỄN PHÍ nếu có subscription. Thanh toán chỉ khi MUA GÓI.",
            "correctEndpoint", "POST /api/driver-subscription"
        ));
    }

    // ==================== READ ENDPOINTS ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments", description = "Get all payments (Admin only)")
    public ResponseEntity<List<Payment>> getAllPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get my payments", description = "Get payments for current driver")
    public ResponseEntity<List<Payment>> getMyPayments() {
        List<Payment> payments = paymentService.getMyPayments();
        return ResponseEntity.ok(payments);
    }

    // ==================== MOMO PAYMENT ====================

    /**
     * POST /api/payment/momo/create : Tạo MoMo payment URL
     * 
     * ✅ WORKFLOW:
     * 1. Driver chọn gói (packageId)
     * 2. Driver gọi API này với packageId
     * 3. System tạo MoMo payment URL
     * 4. Driver redirect đến MoMo để thanh toán (app hoặc web)
     * 5. Sau khi thanh toán thành công, MoMo redirect về /momo-return
     * 6. System TẠO subscription ACTIVE → Driver có thể swap MIỄN PHÍ
     */
    @PostMapping("/momo/create")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Create MoMo payment URL for buying service package",
            description = "Generate MoMo payment URL. Subscription only created AFTER successful payment.")
    public ResponseEntity<Map<String, String>> createMoMoPayment(@RequestParam Long packageId) {
        Map<String, String> result = moMoService.createPaymentUrl(packageId);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/payment/momo-return : MoMo callback sau khi thanh toán
     * 
     * ✅ AUTO-CREATE SUBSCRIPTION:
     * - Verify signature từ MoMo
     * - Nếu thanh toán thành công (resultCode=0) → TẠO subscription ACTIVE
     * - Nếu thanh toán thất bại → KHÔNG tạo subscription
     */
    @GetMapping("/momo-return")
    @Operation(summary = "MoMo return callback",
            description = "Handle MoMo payment result and create subscription if payment succeeds")
    public ResponseEntity<Map<String, Object>> moMoReturn(HttpServletRequest request) {
        Map<String, Object> result = moMoService.handleMoMoReturn(request);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/payment/momo-ipn : MoMo IPN (Instant Payment Notification)
     * Webhook từ MoMo để confirm payment
     */
    @PostMapping("/momo-ipn")
    @Operation(summary = "MoMo IPN callback", description = "MoMo webhook notification")
    public ResponseEntity<Map<String, Object>> moMoIPN(HttpServletRequest request) {
        Map<String, Object> result = moMoService.handleMoMoReturn(request);
        return ResponseEntity.ok(result);
    }
}