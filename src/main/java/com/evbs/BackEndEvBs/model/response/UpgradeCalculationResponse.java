package com.evbs.BackEndEvBs.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO cho API tính toán chi phí nâng cấp gói
 * Theo công thức:
 * - Giá trị hoàn lại = (Lượt chưa dùng) × (Giá/lượt gói cũ)
 * - Phí nâng cấp = Giá gói cũ × 7%
 * - Số tiền cần trả = Giá gói mới + Phí nâng cấp - Giá trị hoàn lại
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeCalculationResponse {

    // ===== THÔNG TIN GÓI HIỆN TẠI =====
    private Long currentSubscriptionId;
    private String currentPackageName;
    private BigDecimal currentPackagePrice;
    private Integer currentMaxSwaps;
    private Integer usedSwaps;
    private Integer remainingSwaps;
    private LocalDate currentStartDate;
    private LocalDate currentEndDate;
    private Integer daysUsed;
    private Integer daysRemaining;

    // ===== THÔNG TIN GÓI MỚI =====
    private Long newPackageId;
    private String newPackageName;
    private BigDecimal newPackagePrice;
    private Integer newMaxSwaps;
    private Integer newDuration;

    // ===== TÍNH TOÁN CHI PHÍ =====
    private BigDecimal pricePerSwapOld;          // Giá/lượt gói cũ (400k/20 = 20k)
    private BigDecimal refundValue;              // Giá trị hoàn lại (15 × 20k = 300k)
    private BigDecimal upgradeFeePercent;        // % phí nâng cấp (7%)
    private BigDecimal upgradeFee;               // Phí nâng cấp (400k × 7% = 28k)
    private BigDecimal totalPaymentRequired;     // Tổng tiền cần trả (528k)

    // ===== THÔNG TIN SAU NÂNG CẤP =====
    private Integer totalSwapsAfterUpgrade;      // Tổng lượt sau nâng cấp (50 lượt)
    private LocalDate newStartDate;              // Ngày bắt đầu gói mới
    private LocalDate newEndDate;                // Ngày kết thúc gói mới

    // ===== SO SÁNH LỢI ÍCH =====
    private BigDecimal pricePerSwapNew;          // Giá/lượt gói mới (800k/50 = 16k)
    private BigDecimal savingsPerSwap;           // Tiết kiệm/lượt (20k - 16k = 4k)
    private String recommendation;               // Gợi ý cho driver

    // ===== THÔNG BÁO =====
    private Boolean canUpgrade;                  // Có thể nâng cấp không
    private String message;                      // Thông báo
}
