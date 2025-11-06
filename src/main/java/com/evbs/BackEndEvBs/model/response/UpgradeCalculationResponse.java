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
 * MÔ HÌNH MỚI - TELCO STYLE (Phương án A):
 * - Gói cũ: BỊ HỦY ngay lập tức (mất hết lượt và ngày còn lại)
 * - Gói mới: Kích hoạt FULL capacity (100% swaps, 100% duration)
 * - Thanh toán: FULL price của gói mới (không hoàn, không phí)
 *
 * Giống như Viettel/Vinaphone: Đổi V90 sang V200
 * → Mất data V90, nhận FULL data V200, trả FULL tiền V200
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

    // ===== TÍNH TOÁN CHI PHÍ (TELCO MODEL) =====
    private BigDecimal pricePerSwapOld;          // Giá/lượt gói cũ (chỉ để tham khảo)
    private BigDecimal refundValue;              // = 0 VNĐ (KHÔNG HOÀN)
    private BigDecimal upgradeFee;               // = 0 VNĐ (KHÔNG PHÍ)
    private BigDecimal totalPaymentRequired;     // = FULL price gói mới
    private BigDecimal estimatedLostValue;       // Giá trị ước tính bị mất (để hiển thị cảnh báo)

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

