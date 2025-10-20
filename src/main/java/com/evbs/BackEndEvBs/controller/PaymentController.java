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

    // ==================== 💳 MOMO PAYMENT ====================

    /**
     * TẠO MOMO PAYMENT URL
     * 
     * WORKFLOW:
     * BUOC 1: Driver chọn gói dịch vụ (packageId)
     * BUOC 2: Driver gọi API này - System tạo MoMo payment URL
     * BUOC 3: Driver redirect đến MoMo app/website
     * BUOC 4: Driver thanh toán trên MoMo
     * BUOC 5: MoMo redirect về /momo-return (callback)
     * BUOC 6: System TỰ ĐỘNG TẠO subscription ACTIVE
     * BUOC 7: Driver có thể swap pin MIỄN PHÍ ngay lập tức
     * 
     * @param packageId ID của gói dịch vụ muốn mua
     * @return Map chứa paymentUrl để redirect driver
     */
    @PostMapping("/momo/create")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Tạo MoMo payment URL để mua gói dịch vụ",
            description = "Tạo URL thanh toán MoMo. Subscription CHỈ được tạo SAU KHI thanh toán thành công.")
    public ResponseEntity<Map<String, String>> createMoMoPayment(@RequestParam Long packageId) {
        Map<String, String> result = moMoService.createPaymentUrl(packageId);
        return ResponseEntity.ok(result);
    }

    /**
     * MOMO CALLBACK - XỬ LÝ SAU KHI THANH TOÁN
     * 
     * QUAN TRỌNG: KHÔNG CẦN TOKEN - Endpoint này public vì callback từ MoMo
     * 
     * AUTO-CREATE SUBSCRIPTION:
     * - Thanh toán thành công (resultCode=0):
     *   + Verify signature từ MoMo
     *   + Tạo Payment record
     *   + Tạo DriverSubscription ACTIVE tự động
     *   + Driver có thể swap miễn phí ngay
     * 
     * - Thanh toán thất bại:
     *   + KHÔNG tạo subscription
     *   + Trả về thông báo lỗi
     * 
     * @param request HttpServletRequest chứa callback params từ MoMo
     * @return Map chứa kết quả xử lý
     */
    @GetMapping("/momo-return")
    @Operation(summary = "MoMo callback - Xử lý kết quả thanh toán",
            description = "Endpoint nhận callback từ MoMo sau khi thanh toán. Tự động tạo subscription nếu thành công.")
    public ResponseEntity<Map<String, Object>> moMoReturn(HttpServletRequest request) {
        Map<String, Object> result = moMoService.handleMoMoReturn(request);
        return ResponseEntity.ok(result);
    }

    /**
     * MOMO IPN (INSTANT PAYMENT NOTIFICATION)
     * 
     * Webhook từ MoMo để confirm payment
     * Xử lý giống /momo-return
     * 
     * QUAN TRỌNG: KHÔNG CẦN TOKEN - Đây là webhook từ MoMo server
     * 
     * @param request HttpServletRequest chứa IPN params từ MoMo
     * @return Map chứa kết quả xử lý
     */
    @PostMapping("/momo-ipn")
    @Operation(summary = "MoMo IPN webhook", 
            description = "Webhook từ MoMo để confirm payment. Xử lý giống momo-return.")
    public ResponseEntity<Map<String, Object>> moMoIPN(HttpServletRequest request) {
        Map<String, Object> result = moMoService.handleMoMoReturn(request);
        return ResponseEntity.ok(result);
    }
}