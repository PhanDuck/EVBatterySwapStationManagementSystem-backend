package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.model.request.DriverSubscriptionRequest;
import com.evbs.BackEndEvBs.model.response.UpgradeCalculationResponse;
import com.evbs.BackEndEvBs.model.response.DowngradeCalculationResponse;
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
            summary = "Calculate upgrade cost",
            description = "Tính toán chi phí nâng cấp gói dịch vụ. " +
                    "Công thức: Tổng tiền = Giá gói mới + Phí nâng cấp (7%) - Giá trị hoàn lại. " +
                    "Driver cần truyền vào packageId của gói muốn nâng cấp."
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
            summary = "Initiate package upgrade",
            description = "Khởi tạo quy trình nâng cấp gói. " +
                    "Sẽ tạo MoMo payment URL với số tiền đã tính toán theo công thức: " +
                    "Tổng tiền = Giá gói mới + 7% phí - Giá trị hoàn lại. " +
                    "Sau khi thanh toán thành công, gói cũ sẽ bị expire và gói mới được kích hoạt với FULL swaps."
    )
    public ResponseEntity<Map<String, String>> initiateUpgrade(
            @RequestParam Long newPackageId,
            @RequestParam(required = false) String redirectUrl
    ) {
        Map<String, String> paymentInfo = moMoService.createUpgradePaymentUrl(newPackageId, redirectUrl);
        return ResponseEntity.ok(paymentInfo);
    }

    // ========================================
    // DOWNGRADE PACKAGE APIs
    // ========================================

    @GetMapping("/downgrade/calculate")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Calculate downgrade conditions",
            description = "Tính toán điều kiện hạ cấp gói dịch vụ. " +
                    "ĐIỀU KIỆN: Số lượt còn lại phải <= MaxSwaps của gói mới. " +
                    "PENALTY: Trừ 10% số lượt còn lại. " +
                    "KHÔNG HOÀN TIỀN (driver đã sử dụng gói cao cấp). " +
                    "Thời hạn gói mới sẽ được kéo dài tương ứng với số lượt còn lại."
    )
    public ResponseEntity<DowngradeCalculationResponse> calculateDowngradeCost(
            @RequestParam Long newPackageId
    ) {
        DowngradeCalculationResponse calculation = driverSubscriptionService.calculateDowngradeCost(newPackageId);
        return ResponseEntity.ok(calculation);
    }

    @PostMapping("/downgrade/confirm")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(
            summary = "Confirm package downgrade",
            description = "Xác nhận hạ cấp gói (KHÔNG CẦN THANH TOÁN). " +
                    "Gói cũ sẽ bị expire, gói mới được kích hoạt với số lượt đã trừ penalty. " +
                    "⚠️ KHÔNG HOÀN TIỀN! Hãy kiểm tra kỹ bằng /downgrade/calculate trước."
    )
    public ResponseEntity<DriverSubscription> confirmDowngrade(
            @RequestParam Long newPackageId
    ) {
        DriverSubscription newSubscription = driverSubscriptionService.downgradeSubscription(newPackageId);
        return ResponseEntity.ok(newSubscription);
    }
}

