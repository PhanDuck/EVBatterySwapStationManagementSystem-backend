package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Payment;
import com.evbs.BackEndEvBs.service.MoMoService;
import com.evbs.BackEndEvBs.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
     * BUOC 5: MoMo redirect về redirectUrl (callback - có thể từ frontend)
     * BUOC 6: System TỰ ĐỘNG TẠO subscription ACTIVE
     * BUOC 7: Driver có thể swap pin MIỄN PHÍ ngay lập tức
     * 
     * @param packageId ID của gói dịch vụ muốn mua
     * @param redirectUrl (Optional) URL để MoMo redirect sau thanh toán. 
     *                    Nếu không truyền, dùng URL mặc định từ config.
     *                    Ví dụ: http://localhost:3000/payment-result
     * @return Map chứa paymentUrl để redirect driver
     */
    @PostMapping("/momo/create")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Tạo MoMo payment URL để mua gói dịch vụ",
            description = "Tạo URL thanh toán MoMo. Frontend có thể truyền redirectUrl tùy chỉnh. Subscription CHỈ được tạo SAU KHI thanh toán thành công.")
    public ResponseEntity<Map<String, String>> createMoMoPayment(
            @RequestParam Long packageId,
            @RequestParam(required = false) String redirectUrl) {
        Map<String, String> result = moMoService.createPaymentUrl(packageId, redirectUrl);
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
    public ResponseEntity<Map<String, Object>> moMoIPN(@RequestBody Map<String, String> momoData) {
        Map<String, Object> result = moMoService.handleMoMoIPN(momoData);
        return ResponseEntity.ok(result);
    }
}