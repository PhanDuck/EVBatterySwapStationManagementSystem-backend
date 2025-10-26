package com.evbs.BackEndEvBs.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO cho API tính toán HẠ CẤP gói
 *
 * LOGIC HẠ CẤP:
 * - CHỈ cho phép nếu remainingSwaps <= newPackageMaxSwaps
 * - KHÔNG HOÀN TIỀN (vì đã sử dụng dịch vụ)
 * - Penalty: Trừ 10% số lượt còn lại
 * - Extension: Kéo dài thời hạn tương ứng với số lượt còn lại
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DowngradeCalculationResponse {

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

    // ===== THÔNG TIN GÓI MỚI (RẺ HƠN) =====
    private Long newPackageId;
    private String newPackageName;
    private BigDecimal newPackagePrice;
    private Integer newMaxSwaps;
    private Integer newDuration;

    // ===== TÍNH TOÁN CHI PHÍ HẠ CẤP =====
    private BigDecimal pricePerSwapOld;          // Giá/lượt gói cũ
    private BigDecimal pricePerSwapNew;          // Giá/lượt gói mới
    private BigDecimal totalPaidForOldPackage;   // Tổng đã trả cho gói cũ
    private BigDecimal noRefund;                 // KHÔNG hoàn tiền = 0
    private BigDecimal downgradePenaltyPercent;  // % penalty (10%)
    private Integer penaltySwaps;                // Số lượt bị trừ do penalty
    private Integer finalRemainingSwaps;         // Số lượt sau khi trừ penalty

    // ===== THÔNG TIN SAU HẠ CẤP =====
    private LocalDate newStartDate;              // = today
    private LocalDate newEndDate;                // Kéo dài dựa trên lượt còn lại
    private Integer extensionDays;               // Số ngày kéo dài

    // ===== CẢNH BÁO & ĐIỀU KIỆN =====
    private Boolean canDowngrade;                // Có thể hạ cấp không
    private String reason;                       // Lý do được/không được
    private String warning;                      // Cảnh báo quan trọng
    private String recommendation;               // Gợi ý
}
