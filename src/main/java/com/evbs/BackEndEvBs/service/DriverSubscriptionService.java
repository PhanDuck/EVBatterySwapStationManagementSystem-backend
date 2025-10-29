package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.response.UpgradeCalculationResponse;
import com.evbs.BackEndEvBs.model.response.DowngradeCalculationResponse;
import com.evbs.BackEndEvBs.model.response.RenewalCalculationResponse;
import com.evbs.BackEndEvBs.repository.DriverSubscriptionRepository;
import com.evbs.BackEndEvBs.repository.ServicePackageRepository;
import com.evbs.BackEndEvBs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverSubscriptionService {

    @Autowired
    private final DriverSubscriptionRepository driverSubscriptionRepository;

    @Autowired
    private final ServicePackageRepository servicePackageRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final UserRepository userRepository;

    @Transactional
    public DriverSubscription createSubscriptionAfterPayment(Long packageId, Long driverId) {
        // Tìm driver by ID thay vì getCurrentUser()
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found with id: " + driverId));

        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("Service package not found with id: " + packageId));

        // Kiểm tra driver có subscription active không
        var activeSubscriptionOpt = driverSubscriptionRepository.findActiveSubscriptionByDriver(driver, LocalDate.now());

        if (activeSubscriptionOpt.isPresent()) {
            DriverSubscription existingSub = activeSubscriptionOpt.get();

            // Still has swaps remaining, not allowed to buy new package
            if (existingSub.getRemainingSwaps() > 0) {
                throw new AuthenticationException(
                        "Driver already has ACTIVE subscription with remaining swaps! " +
                                "Current package: " + existingSub.getServicePackage().getName() + " " +
                                "(remaining " + existingSub.getRemainingSwaps() + " swaps, " +
                                "expires: " + existingSub.getEndDate() + "). "
                );
            }

            // No swaps remaining (remainingSwaps = 0), allow new package purchase
            log.info("Driver {} has active subscription but 0 swaps remaining. Expiring old subscription...",
                    driver.getEmail());

            // Expire gói cũ
            existingSub.setStatus(DriverSubscription.Status.EXPIRED);
            driverSubscriptionRepository.save(existingSub);

            log.info("Old subscription {} expired (was active but had 0 swaps)", existingSub.getId());
        }

        // Create new subscription (no active package or old package expired)
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(servicePackage.getDuration());

        DriverSubscription subscription = new DriverSubscription();
        subscription.setDriver(driver);
        subscription.setServicePackage(servicePackage);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(DriverSubscription.Status.ACTIVE);
        subscription.setRemainingSwaps(servicePackage.getMaxSwaps());

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(subscription);

        log.info("Subscription created after payment (callback): Driver {} -> Package {} ({} swaps, {} VND)",
                driver.getEmail(),
                servicePackage.getName(),
                servicePackage.getMaxSwaps(),
                servicePackage.getPrice());

        return savedSubscription;
    }

    @Transactional(readOnly = true)
    public List<DriverSubscription> getAllSubscriptions() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }
        return driverSubscriptionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DriverSubscription> getMySubscriptions() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can view their subscriptions");
        }
        return driverSubscriptionRepository.findByDriver_Id(currentUser.getId());
    }


    public void deleteSubscription(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        DriverSubscription subscription = driverSubscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver subscription not found with id: " + id));

        // Chuyển status thành CANCELLED
        subscription.setStatus(DriverSubscription.Status.CANCELLED);
        driverSubscriptionRepository.save(subscription);
    }

    // ========================================
    // NÂNG CẤP GÓI (UPGRADE PACKAGE)
    // ========================================

    /**
     * TÍNH TOÁN CHI PHÍ NÂNG CẤP GÓI
     *
     * Công thức (theo yêu cầu của bạn):
     * 1. Giá trị hoàn lại = (Lượt chưa dùng) × (Giá gói cũ / Tổng lượt gói cũ)
     * 2. Phí nâng cấp = Giá gói cũ × 7%
     * 3. Số tiền cần trả = Giá gói mới + Phí nâng cấp - Giá trị hoàn lại
     *
     * Ví dụ:
     * - Gói cũ: 20 lượt = 400,000đ (đã dùng 5, còn 15)
     * - Gói mới: 50 lượt = 800,000đ
     * - Giá trị hoàn lại = 15 × (400,000 / 20) = 15 × 20,000 = 300,000đ
     * - Phí nâng cấp = 400,000 × 7% = 28,000đ
     * - Tổng tiền = 800,000 + 28,000 - 300,000 = 528,000đ
     *
     * @param newPackageId ID của gói mới muốn nâng cấp
     * @return UpgradeCalculationResponse chứa chi tiết tính toán
     */
    @Transactional(readOnly = true)
    public UpgradeCalculationResponse calculateUpgradeCost(Long newPackageId) {
        User currentDriver = authenticationService.getCurrentUser();

        if (currentDriver.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can calculate upgrade cost");
        }

        // 1. Lấy subscription hiện tại
        DriverSubscription currentSub = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentDriver, LocalDate.now())
                .orElseThrow(() -> new NotFoundException(
                        "Bạn chưa có gói dịch vụ nào đang hoạt động. Vui lòng mua gói mới thay vì nâng cấp."
                ));

        ServicePackage currentPackage = currentSub.getServicePackage();

        // 2. Lấy thông tin gói mới
        ServicePackage newPackage = servicePackageRepository.findById(newPackageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + newPackageId));

        // 3. Validation: Chỉ cho phép NÂNG CẤP (gói mới phải đắt hơn hoặc có nhiều lượt hơn)
        if (newPackage.getPrice().compareTo(currentPackage.getPrice()) <= 0
                && newPackage.getMaxSwaps() <= currentPackage.getMaxSwaps()) {
            throw new IllegalArgumentException(
                    "Không thể nâng cấp! Gói mới phải có giá cao hơn hoặc nhiều lượt swap hơn gói hiện tại. " +
                            "Gói hiện tại: " + currentPackage.getPrice() + " VNĐ / " + currentPackage.getMaxSwaps() + " lượt. " +
                            "Gói mới: " + newPackage.getPrice() + " VNĐ / " + newPackage.getMaxSwaps() + " lượt."
            );
        }

        // 4. Tính toán các thông số
        Integer usedSwaps = currentPackage.getMaxSwaps() - currentSub.getRemainingSwaps();
        Integer remainingSwaps = currentSub.getRemainingSwaps();

        LocalDate today = LocalDate.now();
        long daysUsed = ChronoUnit.DAYS.between(currentSub.getStartDate(), today);
        long daysRemaining = ChronoUnit.DAYS.between(today, currentSub.getEndDate());

        // 5. CÔNG THỨC TÍNH TIỀN (theo yêu cầu của bạn)

        // 5.1. Giá mỗi lượt của gói cũ = Giá gói cũ / Tổng lượt
        BigDecimal pricePerSwapOld = currentPackage.getPrice()
                .divide(new BigDecimal(currentPackage.getMaxSwaps()), 2, RoundingMode.HALF_UP);

        // 5.2. Giá trị hoàn lại = Lượt chưa dùng × Giá/lượt
        BigDecimal refundValue = pricePerSwapOld
                .multiply(new BigDecimal(remainingSwaps))
                .setScale(2, RoundingMode.HALF_UP);

        // 5.3. Phí nâng cấp = Giá gói cũ × 7%
        BigDecimal upgradeFeePercent = new BigDecimal("0.07"); // 7%
        BigDecimal upgradeFee = currentPackage.getPrice()
                .multiply(upgradeFeePercent)
                .setScale(2, RoundingMode.HALF_UP);

        // 5.4. Tổng tiền cần trả = Giá gói mới + Phí nâng cấp - Giá trị hoàn lại
        BigDecimal totalPayment = newPackage.getPrice()
                .add(upgradeFee)
                .subtract(refundValue)
                .setScale(2, RoundingMode.HALF_UP);

        // 6. Tính lợi ích
        BigDecimal pricePerSwapNew = newPackage.getPrice()
                .divide(new BigDecimal(newPackage.getMaxSwaps()), 2, RoundingMode.HALF_UP);

        BigDecimal savingsPerSwap = pricePerSwapOld.subtract(pricePerSwapNew);

        // 7. Gợi ý
        String recommendation = generateUpgradeRecommendation(
                currentPackage, newPackage, usedSwaps, remainingSwaps, savingsPerSwap
        );

        // 8. Build response
        return UpgradeCalculationResponse.builder()
                // Gói hiện tại
                .currentSubscriptionId(currentSub.getId())
                .currentPackageName(currentPackage.getName())
                .currentPackagePrice(currentPackage.getPrice())
                .currentMaxSwaps(currentPackage.getMaxSwaps())
                .usedSwaps(usedSwaps)
                .remainingSwaps(remainingSwaps)
                .currentStartDate(currentSub.getStartDate())
                .currentEndDate(currentSub.getEndDate())
                .daysUsed((int) daysUsed)
                .daysRemaining((int) daysRemaining)

                // Gói mới
                .newPackageId(newPackage.getId())
                .newPackageName(newPackage.getName())
                .newPackagePrice(newPackage.getPrice())
                .newMaxSwaps(newPackage.getMaxSwaps())
                .newDuration(newPackage.getDuration())

                // Tính toán
                .pricePerSwapOld(pricePerSwapOld)
                .refundValue(refundValue)
                .upgradeFeePercent(upgradeFeePercent.multiply(new BigDecimal(100))) // 7%
                .upgradeFee(upgradeFee)
                .totalPaymentRequired(totalPayment)

                // Sau nâng cấp
                .totalSwapsAfterUpgrade(newPackage.getMaxSwaps())
                .newStartDate(LocalDate.now())
                .newEndDate(LocalDate.now().plusDays(newPackage.getDuration()))

                // So sánh
                .pricePerSwapNew(pricePerSwapNew)
                .savingsPerSwap(savingsPerSwap)
                .recommendation(recommendation)

                // Status
                .canUpgrade(true)
                .message("Bạn có thể nâng cấp gói dịch vụ. Chi tiết tính toán đã được cung cấp.")
                .build();
    }

    /**
     * XỬ LÝ NÂNG CẤP GÓI SAU KHI THANH TOÁN THÀNH CÔNG
     *
     * Logic:
     * 1. Expire gói cũ (set status = UPGRADED)
     * 2. Tạo gói mới với full swaps
     * 3. Ghi nhận thông tin upgrade vào log
     *
     * @param newPackageId ID gói mới
     * @param driverId ID driver
     * @return DriverSubscription mới sau upgrade
     */
    @Transactional
    public DriverSubscription upgradeSubscriptionAfterPayment(Long newPackageId, Long driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found with id: " + driverId));

        ServicePackage newPackage = servicePackageRepository.findById(newPackageId)
                .orElseThrow(() -> new NotFoundException("Service package not found with id: " + newPackageId));

        // Lấy subscription cũ
        DriverSubscription oldSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(driver, LocalDate.now())
                .orElseThrow(() -> new NotFoundException("No active subscription found for upgrade"));

        ServicePackage oldPackage = oldSubscription.getServicePackage();

        // Log thông tin upgrade
        log.info("UPGRADE PACKAGE - Driver: {} | Old: {} ({} swaps, {} remaining) | New: {} ({} swaps, {} VND)",
                driver.getEmail(),
                oldPackage.getName(),
                oldPackage.getMaxSwaps(),
                oldSubscription.getRemainingSwaps(),
                newPackage.getName(),
                newPackage.getMaxSwaps(),
                newPackage.getPrice()
        );

        // Expire gói cũ (set status EXPIRED thay vì CANCELLED để phân biệt)
        oldSubscription.setStatus(DriverSubscription.Status.EXPIRED);
        oldSubscription.setEndDate(LocalDate.now()); // Kết thúc ngay hôm nay
        driverSubscriptionRepository.save(oldSubscription);

        log.info("Old subscription {} expired. Remaining {} swaps forfeited.",
                oldSubscription.getId(), oldSubscription.getRemainingSwaps());

        // Tạo subscription mới
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(newPackage.getDuration());

        DriverSubscription newSubscription = new DriverSubscription();
        newSubscription.setDriver(driver);
        newSubscription.setServicePackage(newPackage);
        newSubscription.setStartDate(startDate);
        newSubscription.setEndDate(endDate);
        newSubscription.setStatus(DriverSubscription.Status.ACTIVE);
        newSubscription.setRemainingSwaps(newPackage.getMaxSwaps()); // Full swaps của gói mới

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(newSubscription);

        log.info("UPGRADE SUCCESS - New subscription {} created: {} swaps, expires {}",
                savedSubscription.getId(),
                savedSubscription.getRemainingSwaps(),
                savedSubscription.getEndDate()
        );

        return savedSubscription;
    }

    /**
     * GENERATE RECOMMENDATION MESSAGE
     */
    private String generateUpgradeRecommendation(
            ServicePackage currentPackage,
            ServicePackage newPackage,
            Integer usedSwaps,
            Integer remainingSwaps,
            BigDecimal savingsPerSwap
    ) {
        StringBuilder recommendation = new StringBuilder();

        recommendation.append("Phân tích: ");

        if (savingsPerSwap.compareTo(BigDecimal.ZERO) > 0) {
            recommendation.append(String.format(
                    "Gói mới tiết kiệm %,d VNĐ/lượt so với gói cũ. ",
                    savingsPerSwap.intValue()
            ));
        }

        if (remainingSwaps > currentPackage.getMaxSwaps() / 2) {
            recommendation.append(String.format(
                    "Bạn còn %d/%d lượt chưa dùng (%d%%). " +
                            "Nên sử dụng thêm vài lượt trước khi nâng cấp để tối ưu chi phí. ",
                    remainingSwaps,
                    currentPackage.getMaxSwaps(),
                    (remainingSwaps * 100 / currentPackage.getMaxSwaps())
            ));
        } else {
            recommendation.append("Thời điểm nâng cấp hợp lý! ");
        }

        int additionalSwaps = newPackage.getMaxSwaps() - currentPackage.getMaxSwaps();
        if (additionalSwaps > 0) {
            recommendation.append(String.format(
                    "Sau nâng cấp, bạn sẽ có thêm %d lượt swap (%s → %s). ",
                    additionalSwaps,
                    currentPackage.getMaxSwaps(),
                    newPackage.getMaxSwaps()
            ));
        }

        return recommendation.toString();
    }

    // ========================================
    // HẠ CẤP GÓI (DOWNGRADE PACKAGE)
    // ========================================

    /**
     * TÍNH TOÁN CHI PHÍ & ĐIỀU KIỆN HẠ CẤP GÓI
     *
     * BUSINESS RULES:
     * 1. CHO PHÉP nếu: remainingSwaps <= newPackageMaxSwaps
     *    TỪ CHỐI nếu: remainingSwaps > newPackageMaxSwaps (còn quá nhiều lượt)
     *
     * 2. KHÔNG HOÀN TIỀN (driver đã sử dụng dịch vụ cao cấp)
     *
     * 3. PENALTY: Trừ 10% số lượt còn lại
     *    VD: Còn 50 lượt → Trừ 5 lượt → Còn 45 lượt
     *
     * 4. EXTENSION: Kéo dài thời hạn dựa trên lượt còn lại
     *    Công thức: extensionDays = (finalSwaps / newMaxSwaps) × newDuration
     *    VD: 45 lượt / 30 lượt × 30 ngày = 45 ngày
     *
     * @param newPackageId ID của gói mới (RẺ HƠN)
     * @return DowngradeCalculationResponse
     */
    @Transactional(readOnly = true)
    public DowngradeCalculationResponse calculateDowngradeCost(Long newPackageId) {
        User currentDriver = authenticationService.getCurrentUser();

        if (currentDriver.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can calculate downgrade cost");
        }

        // 1. Lấy subscription hiện tại
        DriverSubscription currentSub = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentDriver, LocalDate.now())
                .orElseThrow(() -> new NotFoundException(
                        "Bạn chưa có gói dịch vụ nào đang hoạt động."
                ));

        ServicePackage currentPackage = currentSub.getServicePackage();

        // 2. Lấy thông tin gói mới
        ServicePackage newPackage = servicePackageRepository.findById(newPackageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + newPackageId));

        // 3. Tính toán các thông số
        Integer usedSwaps = currentPackage.getMaxSwaps() - currentSub.getRemainingSwaps();
        Integer remainingSwaps = currentSub.getRemainingSwaps();

        LocalDate today = LocalDate.now();
        long daysUsed = ChronoUnit.DAYS.between(currentSub.getStartDate(), today);
        long daysRemaining = ChronoUnit.DAYS.between(today, currentSub.getEndDate());

        BigDecimal pricePerSwapOld = currentPackage.getPrice()
                .divide(new BigDecimal(currentPackage.getMaxSwaps()), 2, RoundingMode.HALF_UP);

        BigDecimal pricePerSwapNew = newPackage.getPrice()
                .divide(new BigDecimal(newPackage.getMaxSwaps()), 2, RoundingMode.HALF_UP);

        // 4. VALIDATION: Kiểm tra điều kiện hạ cấp

        // 4.1. Gói mới phải RẺ HƠN hoặc ÍT LƯỢT HƠN
        if (newPackage.getPrice().compareTo(currentPackage.getPrice()) >= 0
                && newPackage.getMaxSwaps() >= currentPackage.getMaxSwaps()) {
            return DowngradeCalculationResponse.builder()
                    .canDowngrade(false)
                    .reason("Gói mới phải có giá thấp hơn hoặc ít lượt hơn gói hiện tại. " +
                            "Gói hiện tại: " + currentPackage.getPrice() + " VNĐ / " + currentPackage.getMaxSwaps() + " lượt. " +
                            "Gói mới: " + newPackage.getPrice() + " VNĐ / " + newPackage.getMaxSwaps() + " lượt.")
                    .warning("Đây không phải là hạ cấp! Vui lòng chọn gói rẻ hơn.")
                    .build();
        }

        // 4.2. ĐIỀU KIỆN QUAN TRỌNG: Số lượt còn lại phải <= MaxSwaps của gói mới
        if (remainingSwaps > newPackage.getMaxSwaps()) {
            return DowngradeCalculationResponse.builder()
                    .currentSubscriptionId(currentSub.getId())
                    .currentPackageName(currentPackage.getName())
                    .currentPackagePrice(currentPackage.getPrice())
                    .currentMaxSwaps(currentPackage.getMaxSwaps())
                    .remainingSwaps(remainingSwaps)
                    .newPackageId(newPackage.getId())
                    .newPackageName(newPackage.getName())
                    .newMaxSwaps(newPackage.getMaxSwaps())
                    .canDowngrade(false)
                    .reason(String.format(
                            "KHÔNG THỂ HẠ CẤP! Bạn còn %d lượt swap chưa dùng, " +
                                    "nhưng gói \"%s\" chỉ hỗ trợ tối đa %d lượt. " +
                                    "Vui lòng sử dụng bớt lượt swap (còn <= %d lượt) hoặc chọn gói lớn hơn.",
                            remainingSwaps,
                            newPackage.getName(),
                            newPackage.getMaxSwaps(),
                            newPackage.getMaxSwaps()
                    ))
                    .warning(String.format(
                            "Gợi ý: Sử dụng thêm %d lượt nữa (còn %d lượt) thì bạn có thể hạ cấp xuống gói này.",
                            remainingSwaps - newPackage.getMaxSwaps(),
                            newPackage.getMaxSwaps()
                    ))
                    .build();
        }

        // 5. TÍNH TOÁN HẠ CẤP (đủ điều kiện)

        // 5.1. Penalty: Trừ 10% số lượt còn lại
        BigDecimal penaltyPercent = new BigDecimal("0.10"); // 10%
        Integer penaltySwaps = new BigDecimal(remainingSwaps)
                .multiply(penaltyPercent)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        Integer finalSwaps = remainingSwaps - penaltySwaps;

        // 5.2. Kéo dài thời hạn dựa trên lượt còn lại
        // Công thức: extensionDays = (finalSwaps / newMaxSwaps) × newDuration
        BigDecimal swapRatio = new BigDecimal(finalSwaps)
                .divide(new BigDecimal(newPackage.getMaxSwaps()), 4, RoundingMode.HALF_UP);

        Integer extensionDays = swapRatio
                .multiply(new BigDecimal(newPackage.getDuration()))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        LocalDate newStartDate = LocalDate.now();
        LocalDate newEndDate = newStartDate.plusDays(extensionDays);

        // 6. Generate warning & recommendation
        String warning = String.format(
                "HẠ CẤP KHÔNG HOÀN TIỀN! Bạn đã trả %,d VNĐ cho gói \"%s\". " +
                        "Khi hạ xuống \"%s\", bạn sẽ KHÔNG được hoàn lại số tiền chênh lệch. " +
                        "Ngoài ra, bạn sẽ bị trừ %d lượt swap (penalty 10%%).",
                currentPackage.getPrice().intValue(),
                currentPackage.getName(),
                newPackage.getName(),
                penaltySwaps
        );

        String recommendation = generateDowngradeRecommendation(
                currentPackage, newPackage, remainingSwaps, finalSwaps, extensionDays
        );

        // 7. Build response
        return DowngradeCalculationResponse.builder()
                // Gói hiện tại
                .currentSubscriptionId(currentSub.getId())
                .currentPackageName(currentPackage.getName())
                .currentPackagePrice(currentPackage.getPrice())
                .currentMaxSwaps(currentPackage.getMaxSwaps())
                .usedSwaps(usedSwaps)
                .remainingSwaps(remainingSwaps)
                .currentStartDate(currentSub.getStartDate())
                .currentEndDate(currentSub.getEndDate())
                .daysUsed((int) daysUsed)
                .daysRemaining((int) daysRemaining)

                // Gói mới
                .newPackageId(newPackage.getId())
                .newPackageName(newPackage.getName())
                .newPackagePrice(newPackage.getPrice())
                .newMaxSwaps(newPackage.getMaxSwaps())
                .newDuration(newPackage.getDuration())

                // Tính toán
                .pricePerSwapOld(pricePerSwapOld)
                .pricePerSwapNew(pricePerSwapNew)
                .totalPaidForOldPackage(currentPackage.getPrice())
                .noRefund(BigDecimal.ZERO)
                .downgradePenaltyPercent(penaltyPercent.multiply(new BigDecimal(100)))
                .penaltySwaps(penaltySwaps)
                .finalRemainingSwaps(finalSwaps)

                // Sau hạ cấp
                .newStartDate(newStartDate)
                .newEndDate(newEndDate)
                .extensionDays(extensionDays)

                // Status
                .canDowngrade(true)
                .reason("Bạn đủ điều kiện hạ cấp gói. Vui lòng xem kỹ cảnh báo trước khi quyết định.")
                .warning(warning)
                .recommendation(recommendation)
                .build();
    }

    /**
     * XỬ LÝ HẠ CẤP GÓI (KHÔNG CẦN THANH TOÁN)
     *
     * Logic:
     * 1. Expire gói cũ
     * 2. Tạo gói mới với:
     *    - remainingSwaps = finalSwaps (sau khi trừ penalty)
     *    - endDate = kéo dài tương ứng
     * 3. KHÔNG thu thêm tiền, KHÔNG hoàn tiền
     *
     * @param newPackageId ID gói mới
     * @return DriverSubscription mới sau downgrade
     */
    @Transactional
    public DriverSubscription downgradeSubscription(Long newPackageId) {
        User currentDriver = authenticationService.getCurrentUser();

        if (currentDriver.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can downgrade package");
        }

        // 1. Validate bằng calculate
        DowngradeCalculationResponse calculation = calculateDowngradeCost(newPackageId);

        if (!calculation.getCanDowngrade()) {
            throw new IllegalStateException(
                    "Không thể hạ cấp gói: " + calculation.getReason()
            );
        }

        // 2. Lấy subscription cũ
        DriverSubscription oldSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentDriver, LocalDate.now())
                .orElseThrow(() -> new NotFoundException("No active subscription found"));

        ServicePackage oldPackage = oldSubscription.getServicePackage();
        ServicePackage newPackage = servicePackageRepository.findById(newPackageId)
                .orElseThrow(() -> new NotFoundException("Service package not found"));

        // 3. Log thông tin downgrade
        log.info("DOWNGRADE PACKAGE - Driver: {} | Old: {} ({} swaps, {} remaining) | New: {} (penalty: {} swaps, final: {} swaps, {} days)",
                currentDriver.getEmail(),
                oldPackage.getName(),
                oldPackage.getMaxSwaps(),
                oldSubscription.getRemainingSwaps(),
                newPackage.getName(),
                calculation.getPenaltySwaps(),
                calculation.getFinalRemainingSwaps(),
                calculation.getExtensionDays()
        );

        // 4. Expire gói cũ
        oldSubscription.setStatus(DriverSubscription.Status.EXPIRED);
        oldSubscription.setEndDate(LocalDate.now());
        driverSubscriptionRepository.save(oldSubscription);

        log.info("Old subscription {} expired. {} swaps forfeited (including {} penalty).",
                oldSubscription.getId(),
                oldSubscription.getRemainingSwaps(),
                calculation.getPenaltySwaps()
        );

        // 5. Tạo subscription mới
        DriverSubscription newSubscription = new DriverSubscription();
        newSubscription.setDriver(currentDriver);
        newSubscription.setServicePackage(newPackage);
        newSubscription.setStartDate(calculation.getNewStartDate());
        newSubscription.setEndDate(calculation.getNewEndDate());
        newSubscription.setStatus(DriverSubscription.Status.ACTIVE);
        newSubscription.setRemainingSwaps(calculation.getFinalRemainingSwaps()); // Số lượt sau penalty

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(newSubscription);

        log.info("DOWNGRADE SUCCESS - New subscription {} created: {} swaps, expires {} (extended {} days)",
                savedSubscription.getId(),
                savedSubscription.getRemainingSwaps(),
                savedSubscription.getEndDate(),
                calculation.getExtensionDays()
        );

        return savedSubscription;
    }

    /**
     * GENERATE DOWNGRADE RECOMMENDATION
     */
    private String generateDowngradeRecommendation(
            ServicePackage currentPackage,
            ServicePackage newPackage,
            Integer remainingSwaps,
            Integer finalSwaps,
            Integer extensionDays
    ) {
        StringBuilder rec = new StringBuilder();

        rec.append("Phân tích: ");

        // Cảnh báo về mất tiền
        BigDecimal lostValue = currentPackage.getPrice().subtract(newPackage.getPrice());
        rec.append(String.format(
                "Bạn sẽ KHÔNG được hoàn %,d VNĐ (chênh lệch giữa 2 gói). ",
                lostValue.intValue()
        ));

        // Thông tin về penalty
        int penaltySwaps = remainingSwaps - finalSwaps;
        rec.append(String.format(
                "Bị trừ %d lượt (10%% penalty), còn %d lượt. ",
                penaltySwaps,
                finalSwaps
        ));

        // Thông tin về extension
        rec.append(String.format(
                "Gói mới sẽ kéo dài %d ngày (tính theo %d lượt còn lại). ",
                extensionDays,
                finalSwaps
        ));

        // Gợi ý
        if (remainingSwaps < newPackage.getMaxSwaps() / 2) {
            rec.append("Hợp lý nếu bạn thực sự dùng ít hơn dự kiến. ");
        } else {
            rec.append("Cân nhắc kỹ! Bạn vẫn còn nhiều lượt, có thể dùng hết rồi mua gói mới sẽ tốt hơn. ");
        }

        return rec.toString();
    }

    // ========================================
    // GIA HẠN GÓI (RENEWAL/EXTEND)
    // ========================================

    /**
     * TÍNH TOÁN CHI PHÍ GIA HẠN GÓI (FLEXIBLE RENEWAL)
     *
     * Inspired by NIO & Gogoro best practices:
     *
     * CASE 1: EARLY RENEWAL (còn hạn)
     * - Stack swaps: totalSwaps = remainingSwaps + newMaxSwaps
     * - Stack duration: newEndDate = currentEndDate + newDuration
     * - Discount: 5% (khuyến khích renew sớm)
     * - Bonus: Thêm 10% nếu renew SAME package
     *
     * CASE 2: LATE RENEWAL (hết hạn)
     * - Reset swaps: totalSwaps = newMaxSwaps (mất lượt cũ)
     * - Reset duration: newEndDate = today + newDuration
     * - No discount
     *
     * @param renewalPackageId ID của gói muốn gia hạn (có thể khác gói hiện tại)
     * @return RenewalCalculationResponse
     */
    @Transactional(readOnly = true)
    public RenewalCalculationResponse calculateRenewalCost(Long renewalPackageId) {
        User currentDriver = authenticationService.getCurrentUser();

        if (currentDriver.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can calculate renewal cost");
        }

        // 1. Lấy subscription mới nhất của driver (theo subscriptionId - gói gần nhất được tạo)
        List<DriverSubscription> allSubs = driverSubscriptionRepository.findByDriver_Id(currentDriver.getId());

        if (allSubs.isEmpty()) {
            throw new NotFoundException("Bạn chưa có gói dịch vụ nào. Vui lòng mua gói mới thay vì gia hạn.");
        }

        // Lấy subscription mới nhất dựa trên ID (gói được tạo sau cùng)
        DriverSubscription latestSub = allSubs.stream()
                .max((s1, s2) -> s1.getId().compareTo(s2.getId()))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy subscription"));

        // Kiểm tra gói mới nhất có bị CANCELLED không
        if (latestSub.getStatus() == DriverSubscription.Status.CANCELLED) {
            throw new IllegalArgumentException(
                    "Gói gần nhất của bạn đã bị hủy. Vui lòng mua gói mới thay vì gia hạn."
            );
        }

        // Chỉ chấp nhận renewal nếu gói mới nhất là ACTIVE hoặc EXPIRED
        if (latestSub.getStatus() != DriverSubscription.Status.ACTIVE
                && latestSub.getStatus() != DriverSubscription.Status.EXPIRED) {
            throw new IllegalArgumentException(
                    "Không thể gia hạn gói với trạng thái: " + latestSub.getStatus()
            );
        }

        DriverSubscription currentSub = latestSub;

        ServicePackage currentPackage = currentSub.getServicePackage();

        // 2. Lấy thông tin gói muốn gia hạn
        ServicePackage renewalPackage = servicePackageRepository.findById(renewalPackageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + renewalPackageId));

        // 2.1. VALIDATION: Chỉ cho phép gia hạn CÙNG GÓI
        if (!currentPackage.getId().equals(renewalPackageId)) {
            throw new IllegalArgumentException(
                    "KHÔNG THỂ GIA HẠN! Bạn chỉ được gia hạn cùng gói hiện tại. " +
                            "Gói hiện tại: \"" + currentPackage.getName() + "\" (ID: " + currentPackage.getId() + "). " +
                            "Gói bạn chọn: \"" + renewalPackage.getName() + "\" (ID: " + renewalPackageId + "). " +
                            "Nếu muốn đổi gói khác, vui lòng sử dụng chức năng NÂNG CẤP hoặc HẠ CẤP gói."
            );
        }

        // 3. Xác định loại renewal: EARLY hay LATE
        LocalDate today = LocalDate.now();
        boolean isExpired = currentSub.getEndDate().isBefore(today);
        String renewalType = isExpired ? "LATE" : "EARLY";

        long daysRemaining = isExpired ? 0 : ChronoUnit.DAYS.between(today, currentSub.getEndDate());
        Integer remainingSwaps = currentSub.getRemainingSwaps();

        // 4. Luôn renew same package (đã validate ở trên)
        boolean isSamePackage = true;

        // 5. TÍNH TOÁN CHI PHÍ
        BigDecimal originalPrice = renewalPackage.getPrice();
        BigDecimal earlyDiscount = BigDecimal.ZERO;
        BigDecimal samePackageDiscount = BigDecimal.ZERO;

        // 5.1. Early renewal discount (5%)
        if (!isExpired) {
            earlyDiscount = originalPrice.multiply(new BigDecimal("0.05"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 5.2. Same package bonus (10%) - Luôn được áp dụng vì chỉ cho phép gia hạn cùng gói
        samePackageDiscount = originalPrice.multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_UP);

        // 5.3. Tổng discount
        BigDecimal totalDiscount = earlyDiscount.add(samePackageDiscount);
        BigDecimal finalPrice = originalPrice.subtract(totalDiscount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 6. TÍNH TOÁN SAU GIA HẠN
        Integer totalSwaps;
        LocalDate newStartDate;
        LocalDate newEndDate;
        Integer totalDuration;
        Integer stackedSwaps = 0;

        if (isExpired) {
            // LATE RENEWAL - Reset hoàn toàn
            totalSwaps = renewalPackage.getMaxSwaps();
            newStartDate = today;
            newEndDate = today.plusDays(renewalPackage.getDuration());
            totalDuration = renewalPackage.getDuration();
        } else {
            // EARLY RENEWAL - Stack swaps & duration
            totalSwaps = remainingSwaps + renewalPackage.getMaxSwaps();
            stackedSwaps = remainingSwaps; // Số lượt được giữ lại
            newStartDate = today;
            newEndDate = currentSub.getEndDate().plusDays(renewalPackage.getDuration());
            totalDuration = (int) ChronoUnit.DAYS.between(today, newEndDate);
        }

        // 7. Tính giá/lượt và tiết kiệm
        BigDecimal pricePerSwap = finalPrice.divide(new BigDecimal(renewalPackage.getMaxSwaps()), 2, RoundingMode.HALF_UP);
        BigDecimal savingsAmount = totalDiscount;

        // 8. Generate recommendation
        String recommendation = generateRenewalRecommendation(
                currentPackage, renewalPackage, isExpired, isSamePackage,
                totalDiscount, stackedSwaps
        );

        String message = isExpired
                ? "Gói của bạn đã hết hạn. Gia hạn ngay để tiếp tục sử dụng dịch vụ! (Chỉ được gia hạn cùng gói)"
                : String.format("Bạn có thể gia hạn sớm và nhận ưu đãi! Còn %d ngày và %d lượt swap. (Chỉ được gia hạn cùng gói)",
                daysRemaining, remainingSwaps);

        // 9. Build response
        return RenewalCalculationResponse.builder()
                // Current subscription
                .currentSubscriptionId(currentSub.getId())
                .currentPackageName(currentPackage.getName())
                .currentPackagePrice(currentPackage.getPrice())
                .currentMaxSwaps(currentPackage.getMaxSwaps())
                .remainingSwaps(remainingSwaps)
                .currentStartDate(currentSub.getStartDate())
                .currentEndDate(currentSub.getEndDate())
                .daysRemaining((int) daysRemaining)
                .isExpired(isExpired)

                // Renewal package
                .renewalPackageId(renewalPackage.getId())
                .renewalPackageName(renewalPackage.getName())
                .renewalPackagePrice(renewalPackage.getPrice())
                .renewalMaxSwaps(renewalPackage.getMaxSwaps())
                .renewalDuration(renewalPackage.getDuration())

                // Pricing
                .renewalType(renewalType)
                .isSamePackage(isSamePackage)
                .earlyRenewalDiscount(earlyDiscount)
                .samePackageDiscount(samePackageDiscount)
                .totalDiscount(totalDiscount)
                .originalPrice(originalPrice)
                .finalPrice(finalPrice)

                // After renewal
                .totalSwapsAfterRenewal(totalSwaps)
                .newStartDate(newStartDate)
                .newEndDate(newEndDate)
                .totalDuration(totalDuration)
                .stackedSwaps(stackedSwaps)

                // Info
                .canRenew(true)
                .message(message)
                .recommendation(recommendation)
                .pricePerSwap(pricePerSwap)
                .savingsAmount(savingsAmount)
                .build();
    }

    /**
     * XỬ LÝ GIA HẠN SAU KHI THANH TOÁN THÀNH CÔNG
     *
     * @param renewalPackageId ID gói gia hạn
     * @param driverId ID driver
     * @return DriverSubscription mới sau renewal
     */
    @Transactional
    public DriverSubscription renewSubscriptionAfterPayment(Long renewalPackageId, Long driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found with id: " + driverId));

        ServicePackage renewalPackage = servicePackageRepository.findById(renewalPackageId)
                .orElseThrow(() -> new NotFoundException("Service package not found with id: " + renewalPackageId));

        // Lấy subscription mới nhất của driver (theo subscriptionId - gói gần nhất được tạo)
        List<DriverSubscription> allSubs = driverSubscriptionRepository.findByDriver_Id(driverId);
        DriverSubscription oldSubscription = allSubs.stream()
                .max((s1, s2) -> s1.getId().compareTo(s2.getId()))
                .orElse(null);

        // Kiểm tra nếu gói mới nhất bị CANCELLED
        if (oldSubscription != null && oldSubscription.getStatus() == DriverSubscription.Status.CANCELLED) {
            throw new IllegalArgumentException(
                    "Gói gần nhất của bạn đã bị hủy. Không thể gia hạn. Vui lòng mua gói mới."
            );
        }

        // Chỉ cho phép renewal nếu gói mới nhất là ACTIVE hoặc EXPIRED
        if (oldSubscription != null
                && oldSubscription.getStatus() != DriverSubscription.Status.ACTIVE
                && oldSubscription.getStatus() != DriverSubscription.Status.EXPIRED) {
            throw new IllegalArgumentException(
                    "Không thể gia hạn gói với trạng thái: " + oldSubscription.getStatus()
            );
        }

        // VALIDATION: Chỉ cho phép gia hạn CÙNG GÓI
        if (oldSubscription != null) {
            ServicePackage oldPackage = oldSubscription.getServicePackage();
            if (!oldPackage.getId().equals(renewalPackageId)) {
                throw new IllegalArgumentException(
                        "KHÔNG THỂ GIA HẠN! Bạn chỉ được gia hạn cùng gói hiện tại. " +
                                "Gói hiện tại: \"" + oldPackage.getName() + "\" (ID: " + oldPackage.getId() + "). " +
                                "Gói bạn chọn: \"" + renewalPackage.getName() + "\" (ID: " + renewalPackageId + "). " +
                                "Nếu muốn đổi gói khác, vui lòng sử dụng chức năng NÂNG CẤP hoặc HẠ CẤP gói."
                );
            }
        }

        LocalDate today = LocalDate.now();
        LocalDate newStartDate = today;
        LocalDate newEndDate;

        Integer stackedSwaps = 0;

        if (oldSubscription != null) {
            ServicePackage oldPackage = oldSubscription.getServicePackage();
            boolean isExpired = oldSubscription.getEndDate().isBefore(today);

            log.info("RENEWAL - Driver: {} | Old: {} (expired: {}, {} swaps remaining) | New: {}",
                    driver.getEmail(),
                    oldPackage.getName(),
                    isExpired,
                    oldSubscription.getRemainingSwaps(),
                    renewalPackage.getName()
            );

            if (!isExpired) {
                // EARLY RENEWAL - Stack swaps & duration
                stackedSwaps = oldSubscription.getRemainingSwaps();
                newEndDate = oldSubscription.getEndDate().plusDays(renewalPackage.getDuration());
            } else {
                // LATE RENEWAL - Reset
                newEndDate = today.plusDays(renewalPackage.getDuration());
            }

            // Expire gói cũ
            oldSubscription.setStatus(DriverSubscription.Status.EXPIRED);
            oldSubscription.setEndDate(today);
            driverSubscriptionRepository.save(oldSubscription);

            log.info("Old subscription {} expired.", oldSubscription.getId());
        } else {
            // Lần đầu mua gói (không có subscription cũ)
            newEndDate = today.plusDays(renewalPackage.getDuration());
        }

        // Tạo subscription mới
        DriverSubscription newSubscription = new DriverSubscription();
        newSubscription.setDriver(driver);
        newSubscription.setServicePackage(renewalPackage);
        newSubscription.setStartDate(newStartDate);
        newSubscription.setEndDate(newEndDate);
        newSubscription.setStatus(DriverSubscription.Status.ACTIVE);

        // STACK swaps nếu early renewal
        Integer totalSwaps = renewalPackage.getMaxSwaps() + stackedSwaps;
        newSubscription.setRemainingSwaps(totalSwaps);

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(newSubscription);

        log.info("RENEWAL SUCCESS - New subscription {} created: {} swaps (stacked: {}), expires {}",
                savedSubscription.getId(),
                savedSubscription.getRemainingSwaps(),
                stackedSwaps,
                savedSubscription.getEndDate()
        );

        return savedSubscription;
    }

    /**
     * GENERATE RENEWAL RECOMMENDATION
     */
    private String generateRenewalRecommendation(
            ServicePackage currentPackage,
            ServicePackage renewalPackage,
            boolean isExpired,
            boolean isSamePackage,
            BigDecimal totalDiscount,
            Integer stackedSwaps
    ) {
        StringBuilder rec = new StringBuilder();

        rec.append("Phân tích: ");

        if (isExpired) {
            rec.append("Gói đã hết hạn! Gia hạn ngay để không bỏ lỡ dịch vụ. ");
            rec.append(String.format("Gia hạn gói \"%s\" để tiết kiệm 10%% (-%,d VNĐ). ",
                    renewalPackage.getName(), totalDiscount.intValue()));
        } else {
            rec.append("Gia hạn sớm! ");
            if (stackedSwaps > 0) {
                rec.append(String.format("Bạn sẽ giữ được %d lượt chưa dùng + thêm %d lượt mới = %d lượt! ",
                        stackedSwaps, renewalPackage.getMaxSwaps(), stackedSwaps + renewalPackage.getMaxSwaps()));
            }

            if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
                rec.append(String.format("Tiết kiệm %,d VNĐ nhờ ưu đãi (5%% early + 10%% same package). ",
                        totalDiscount.intValue()));
            }
        }

        rec.append("🎯 Renew đúng gói đang dùng - Lựa chọn thông minh! ");

        return rec.toString();
    }
}