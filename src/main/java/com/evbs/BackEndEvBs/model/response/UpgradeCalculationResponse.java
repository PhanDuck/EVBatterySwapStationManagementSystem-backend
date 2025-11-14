package com.evbs.BackEndEvBs.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO cho API tính toán chi phí nâng cấp gói
 *
 * MÔ HÌNH MỚI - CÓ HOÀN LẠI GIÁ TRỊ:
 * - Gói cũ: BỊ HỦY ngay lập tức
 * - Giá trị hoàn lại: Tính theo lượt chưa dùng = (Lượt chưa dùng) × (Giá gói cũ / Tổng lượt gói cũ)
 * - Gói mới: Kích hoạt FULL capacity (100% swaps, 100% duration)
 * - Thanh toán: Giá gói mới - Giá trị hoàn lại
 *
 * VÍ DỤ:
 * - Gói cũ: 20 lượt = 400,000đ (đã dùng 5, còn 15)
 * - Gói mới: 50 lượt = 800,000đ
 * - Giá trị hoàn lại = 15 × (400,000 / 20) = 15 × 20,000 = 300,000đ
 * - Tổng tiền = 800,000 - 300,000 = 500,000đ
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
    private BigDecimal pricePerSwapOld;          // Giá/lượt gói cũ (= Giá gói cũ / Tổng lượt)
    private BigDecimal refundValue;              // Giá trị hoàn lại = (Lượt chưa dùng) × (Giá/lượt)
    private BigDecimal upgradeFee;               // Phí nâng cấp (hiện tại = 0)
    private BigDecimal totalPaymentRequired;     // Số tiền cần trả = Giá gói mới - Giá trị hoàn lại
    private BigDecimal estimatedLostValue;       // Giá trị ngày còn lại bị mất (để hiển thị)

    // ===== THÔNG TIN SAU NÂNG CẤP =====
    private Integer totalSwapsAfterUpgrade;      // = newMaxSwaps (FULL 100%)
    private LocalDate newStartDate;              // = Hôm nay
    private LocalDate newEndDate;                // = Hôm nay + newDuration

    // ===== SO SÁNH LỢI ÍCH =====
    private BigDecimal pricePerSwapNew;          // Giá/lượt gói mới
    private BigDecimal savingsPerSwap;           // Tiết kiệm/lượt (nếu có)

    // ===== THÔNG BÁO & GỢI Ý =====
    private Boolean canUpgrade;                  // Có thể nâng cấp không
    private String message;                      // Thông báo chính
    private String warning;                      // Cảnh báo về những gì sẽ mất
    private String recommendation;               // Phân tích chi tiết & gợi ý
}

