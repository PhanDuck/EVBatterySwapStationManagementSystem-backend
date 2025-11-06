package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.model.request.DriverSubscriptionRequest;
import com.evbs.BackEndEvBs.model.response.UpgradeCalculationResponse;
import com.evbs.BackEndEvBs.model.response.RenewalCalculationResponse;
import com.evbs.BackEndEvBs.service.DriverSubscriptionService;
import com.evbs.BackEndEvBs.service.MoMoService;
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

    @Autowired
    private MoMoService moMoService;

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete subscription", description = "Delete subscription (Admin only)")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        driverSubscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================
    // UPGRADE PACKAGE APIs
    // ========================================

    @GetMapping("/upgrade/calculate")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Calculate upgrade cost (Telco Model)",
            description = "Tính toán chi phí nâng cấp gói dịch vụ theo mô hình Telco (Viettel/Vinaphone). " +
                    "\n\n**MÔ HÌNH TELCO:**" +
                    "\n- Gói cũ: BỊ HỦY ngay (mất hết lượt và ngày còn lại)" +
                    "\n- Gói mới: Kích hoạt FULL (100% swaps, 100% duration)" +
                    "\n- Thanh toán: FULL PRICE gói mới (không hoàn tiền, không phí)" +
                    "\n\n**VÍ DỤ:**" +
                    "\n- Gói cũ: Basic (400k/20 lượt), còn 15 lượt, 20 ngày" +
                    "\n- Nâng lên: Premium (800k/50 lượt)" +
                    "\n- Bạn mất: 15 lượt + 20 ngày (~300k)" +
                    "\n- Bạn trả: 800k (FULL)" +
                    "\n- Bạn nhận: 50 lượt FULL + 30 ngày MỚI" +
                    "\n\n**CẢNH BÁO:** Đọc kỹ phần 'warning' và 'recommendation' trước khi quyết định!"
    )
    public ResponseEntity<UpgradeCalculationResponse> calculateUpgradeCost(
            @RequestParam Long newPackageId
    ) {
        UpgradeCalculationResponse calculation = driverSubscriptionService.calculateUpgradeCost(newPackageId);
        return ResponseEntity.ok(calculation);
    }

    @PostMapping("/upgrade/initiate")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Initiate package upgrade (Telco Model)",
            description = "Khởi tạo quy trình nâng cấp gói theo mô hình Telco. " +
                    "\n\n**QUY TRÌNH:**" +
                    "\n1. Tạo MoMo payment URL với số tiền = FULL PRICE gói mới" +
                    "\n2. Driver thanh toán qua MoMo" +
                    "\n3. Sau khi thanh toán thành công:" +
                    "\n   - Gói cũ: Status = CANCELLED (mất tất cả)" +
                    "\n   - Gói mới: Status = ACTIVE, swaps = FULL, duration = FULL" +
                    "\n\n**LƯU Ý:** Gọi /upgrade/calculate trước để xem cảnh báo chi tiết!"
    )
    public ResponseEntity<Map<String, String>> initiateUpgrade(
            @RequestParam Long newPackageId,
            @RequestParam(required = false) String redirectUrl
    ) {
        Map<String, String> paymentInfo = moMoService.createUpgradePaymentUrl(newPackageId, redirectUrl);
        return ResponseEntity.ok(paymentInfo);
    }

    // ========================================
    // RENEWAL/GIA HẠN GÓI ENDPOINTS
    // ========================================

    @GetMapping("/renewal/calculate")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Calculate renewal cost (Flexible Renewal)",
            description = "Tính toán chi phí gia hạn gói (có thể renew sang gói khác). " +
                    "\n\n**EARLY RENEWAL** (còn hạn):" +
                    "\n- Discount 5% + thêm 10% nếu renew SAME package" +
                    "\n- STACK swaps: giữ lại lượt cũ + thêm lượt mới" +
                    "\n- STACK duration: endDate = currentEndDate + newDuration" +
                    "\n\n**LATE RENEWAL** (hết hạn):" +
                    "\n- No discount" +
                    "\n- RESET swaps: chỉ có lượt mới (mất lượt cũ)" +
                    "\n- RESET duration: endDate = today + newDuration" +
                    "\n\n**Khuyến nghị**: Renew sớm để được discount và giữ lại lượt chưa dùng!"
    )
    public ResponseEntity<RenewalCalculationResponse> calculateRenewalCost(
            @RequestParam Long renewalPackageId
    ) {
        RenewalCalculationResponse response = driverSubscriptionService.calculateRenewalCost(renewalPackageId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/renewal/initiate")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Initiate renewal payment",
            description = "Tạo URL thanh toán MoMo để gia hạn gói. " +
                    "Sau khi thanh toán thành công, gói cũ sẽ được expire và gói mới tự động kích hoạt. " +
                    "\n\n**Lưu ý**: Nên gọi /renewal/calculate trước để xem chi tiết discount và số lượt sau renewal."
    )
    public ResponseEntity<Map<String, String>> initiateRenewal(
            @RequestParam Long renewalPackageId,
            @RequestParam(required = false) String redirectUrl
    ) {
        Map<String, String> paymentInfo = moMoService.createRenewalPaymentUrl(renewalPackageId, redirectUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentInfo);
    }
}


