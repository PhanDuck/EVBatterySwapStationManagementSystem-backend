package com.evbs.BackEndEvBs.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO cho API tính toán GIA HẠN gói
 *
 * LOGIC GIA HẠN (FLEXIBLE RENEWAL - giống NIO & Gogoro):
 *
 * 1. EARLY RENEWAL (còn hạn):
 *    - Stack swaps: newSwaps = remainingSwaps + newMaxSwaps
 *    - Stack duration: newEndDate = currentEndDate + newDuration
 *    - Discount: 5% nếu renew sớm
 *    - Discount thêm: 10% nếu renew SAME package
 *
 * 2. LATE RENEWAL (hết hạn):
 *    - Reset swaps: newSwaps = newMaxSwaps (mất lượt cũ)
 *    - Reset duration: newEndDate = today + newDuration
 *    - No discount
 *
 * 3. SAME PACKAGE BONUS:
 *    - Giảm 10% khi renew đúng gói đang dùng
 *    - Khuyến khích loyalty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewalCalculationResponse {

    // ===== THÔNG TIN GÓI HIỆN TẠI =====
    private Long currentSubscriptionId;
    private String currentPackageName;
    private BigDecimal currentPackagePrice;
    private Integer currentMaxSwaps;
    private Integer remainingSwaps;
    private LocalDate currentStartDate;
    private LocalDate currentEndDate;
    private Integer daysRemaining;
    private Boolean isExpired;

    // ===== THÔNG TIN GÓI GIA HẠN =====
    private Long renewalPackageId;
    private String renewalPackageName;
    private BigDecimal renewalPackagePrice;
    private Integer renewalMaxSwaps;
    private Integer renewalDuration;

    // ===== TÍNH TOÁN CHI PHÍ =====
    private String renewalType;                  // "EARLY" hoặc "LATE"
    private Boolean isSamePackage;               // Có renew đúng gói không
    private BigDecimal earlyRenewalDiscount;     // 5% nếu còn hạn
    private BigDecimal samePackageDiscount;      // 10% nếu renew same
    private BigDecimal totalDiscount;            // Tổng discount
    private BigDecimal originalPrice;            // Giá gốc
    private BigDecimal finalPrice;               // Giá sau discount

    // ===== SAU KHI GIA HẠN =====
    private Integer totalSwapsAfterRenewal;      // Tổng lượt sau renew
    private LocalDate newStartDate;              // Ngày bắt đầu
    private LocalDate newEndDate;                // Ngày kết thúc
    private Integer totalDuration;               // Tổng số ngày
    private Integer stackedSwaps;                // Lượt được cộng dồn (nếu early)

    // ===== THÔNG TIN & GỢI Ý =====
    private Boolean canRenew;                    // Có thể gia hạn không
    private String message;                      // Thông báo
    private String recommendation;               // Gợi ý
    private BigDecimal pricePerSwap;             // Giá/lượt sau renew
    private BigDecimal savingsAmount;            // Số tiền tiết kiệm được
}
