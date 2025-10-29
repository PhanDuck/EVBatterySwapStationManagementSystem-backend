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
        // Tìm tài xế theo ID (thay vì getCurrentUser)
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài xế với ID: " + driverId));

        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + packageId));

        // Kiểm tra tài xế có gói đăng ký đang hoạt động không
        var activeSubscriptionOpt = driverSubscriptionRepository.findActiveSubscriptionByDriver(driver, LocalDate.now());

        if (activeSubscriptionOpt.isPresent()) {
            DriverSubscription existingSub = activeSubscriptionOpt.get();

            // Nếu vẫn còn lượt đổi pin thì không được mua gói mới
            if (existingSub.getRemainingSwaps() > 0) {
                throw new AuthenticationException(
                        "Tài xế hiện đang có gói đăng ký còn hiệu lực và vẫn còn lượt đổi pin! " +
                                "Gói hiện tại: " + existingSub.getServicePackage().getName() + " " +
                                "(còn lại " + existingSub.getRemainingSwaps() + " lượt đổi, " +
                                "hết hạn vào: " + existingSub.getEndDate() + ")."
                );
            }

            // Nếu đã hết lượt (remainingSwaps = 0), cho phép mua gói mới
            log.info("Tài xế {} đang có gói hoạt động nhưng đã hết lượt đổi pin. Đang hết hạn gói cũ...",
                    driver.getEmail());

            // Hết hạn gói cũ
            existingSub.setStatus(DriverSubscription.Status.EXPIRED);
            driverSubscriptionRepository.save(existingSub);

            log.info("Gói đăng ký cũ {} đã được hết hạn (đang hoạt động nhưng không còn lượt đổi).", existingSub.getId());
        }

        // Tạo gói đăng ký mới (không có gói hoạt động hoặc gói cũ đã hết hạn)
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

        log.info("Đã tạo gói đăng ký mới sau khi thanh toán (callback): Tài xế {} -> Gói {} ({} lượt đổi, {} VND).",
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
            throw new AuthenticationException("Quyền truy cập bị từ chối. Yêu cầu vai trò quản trị viên.");
        }
        return driverSubscriptionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DriverSubscription> getMySubscriptions() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Chỉ có tài xế mới có thể xem đăng ký của họ");
        }
        return driverSubscriptionRepository.findByDriver_Id(currentUser.getId());
    }


    public void deleteSubscription(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Quyền truy cập bị từ chối. Yêu cầu vai trò quản trị viên.");
        }

        DriverSubscription subscription = driverSubscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đăng ký trình điều khiển có id: " + id));

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
            throw new AuthenticationException("Chỉ có tài xế mới có thể tính toán chi phí nâng cấp");
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
        // Tìm thông tin tài xế
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài xế với ID: " + driverId));

        // Tìm thông tin gói dịch vụ mới
        ServicePackage newPackage = servicePackageRepository.findById(newPackageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + newPackageId));

        // Lấy gói đăng ký hiện tại (đang hoạt động)
        DriverSubscription oldSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(driver, LocalDate.now())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký đang hoạt động để nâng cấp."));

        ServicePackage oldPackage = oldSubscription.getServicePackage();

        // Ghi log thông tin nâng cấp
        log.info("NÂNG CẤP GÓI - Tài xế: {} | Gói cũ: {} ({} lượt đổi, còn lại {}) | Gói mới: {} ({} lượt đổi, {} VND)",
                driver.getEmail(),
                oldPackage.getName(),
                oldPackage.getMaxSwaps(),
                oldSubscription.getRemainingSwaps(),
                newPackage.getName(),
                newPackage.getMaxSwaps(),
                newPackage.getPrice()
        );

        // Hết hạn gói cũ (đánh dấu EXPIRED để phân biệt với CANCELLED)
        oldSubscription.setStatus(DriverSubscription.Status.EXPIRED);
        oldSubscription.setEndDate(LocalDate.now()); // Kết thúc ngay hôm nay
        driverSubscriptionRepository.save(oldSubscription);

        log.info("Gói cũ {} đã được hết hạn. {} lượt đổi còn lại sẽ bị hủy bỏ.",
                oldSubscription.getId(), oldSubscription.getRemainingSwaps());

        // Tạo gói đăng ký mới
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(newPackage.getDuration());

        DriverSubscription newSubscription = new DriverSubscription();
        newSubscription.setDriver(driver);
        newSubscription.setServicePackage(newPackage);
        newSubscription.setStartDate(startDate);
        newSubscription.setEndDate(endDate);
        newSubscription.setStatus(DriverSubscription.Status.ACTIVE);
        newSubscription.setRemainingSwaps(newPackage.getMaxSwaps()); // Số lượt đổi tối đa của gói mới

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(newSubscription);

        log.info("NÂNG CẤP THÀNH CÔNG - Đã tạo gói đăng ký mới {}: {} lượt đổi, hết hạn vào {}.",
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
            throw new AuthenticationException("Chỉ tài xế mới có thể tính toán chi phí hạ cấp gói.");
        }

        // 1. Lấy gói đăng ký hiện tại
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

        // 4. Kiểm tra điều kiện hạ cấp

        // 4.1. Gói mới phải RẺ HƠN hoặc ÍT LƯỢT HƠN
        if (newPackage.getPrice().compareTo(currentPackage.getPrice()) >= 0
                && newPackage.getMaxSwaps() >= currentPackage.getMaxSwaps()) {
            return DowngradeCalculationResponse.builder()
                    .canDowngrade(false)
                    .reason("Gói mới phải có giá thấp hơn hoặc có số lượt đổi ít hơn gói hiện tại. " +
                            "Gói hiện tại: " + currentPackage.getPrice() + " VNĐ / " + currentPackage.getMaxSwaps() + " lượt. " +
                            "Gói mới: " + newPackage.getPrice() + " VNĐ / " + newPackage.getMaxSwaps() + " lượt.")
                    .warning("Đây không phải là hạ cấp! Vui lòng chọn gói có giá thấp hơn.")
                    .build();
        }

        // 4.2. Kiểm tra: Số lượt còn lại phải <= số lượt tối đa của gói mới
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
                            "KHÔNG THỂ HẠ CẤP! Bạn còn %d lượt đổi pin chưa sử dụng, " +
                                    "trong khi gói \"%s\" chỉ hỗ trợ tối đa %d lượt. " +
                                    "Vui lòng sử dụng bớt (còn ≤ %d lượt) hoặc chọn gói lớn hơn.",
                            remainingSwaps,
                            newPackage.getName(),
                            newPackage.getMaxSwaps(),
                            newPackage.getMaxSwaps()
                    ))
                    .warning(String.format(
                            "Gợi ý: Hãy sử dụng thêm %d lượt nữa (còn lại %d lượt) để có thể hạ cấp xuống gói này.",
                            remainingSwaps - newPackage.getMaxSwaps(),
                            newPackage.getMaxSwaps()
                    ))
                    .build();
        }

        // 5. Đủ điều kiện hạ cấp → Tiến hành tính toán

        // 5.1. Phí phạt (penalty): Trừ 10% số lượt còn lại
        BigDecimal penaltyPercent = new BigDecimal("0.10"); // 10%
        Integer penaltySwaps = new BigDecimal(remainingSwaps)
                .multiply(penaltyPercent)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        Integer finalSwaps = remainingSwaps - penaltySwaps;

        // 5.2. Tính thời hạn gói mới dựa trên số lượt còn lại
        // Công thức: extensionDays = (finalSwaps / newMaxSwaps) × newDuration
        BigDecimal swapRatio = new BigDecimal(finalSwaps)
                .divide(new BigDecimal(newPackage.getMaxSwaps()), 4, RoundingMode.HALF_UP);

        Integer extensionDays = swapRatio
                .multiply(new BigDecimal(newPackage.getDuration()))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        LocalDate newStartDate = LocalDate.now();
        LocalDate newEndDate = newStartDate.plusDays(extensionDays);

        // 6. Cảnh báo & khuyến nghị
        String warning = String.format(
                "⚠️ HẠ CẤP KHÔNG HOÀN TIỀN! Bạn đã thanh toán %,d VNĐ cho gói \"%s\". " +
                        "Khi hạ xuống \"%s\", bạn sẽ KHÔNG được hoàn lại phần chênh lệch. " +
                        "Ngoài ra, bạn sẽ bị trừ %d lượt đổi pin (phí phạt 10%%).",
                currentPackage.getPrice().intValue(),
                currentPackage.getName(),
                newPackage.getName(),
                penaltySwaps
        );

        String recommendation = generateDowngradeRecommendation(
                currentPackage, newPackage, remainingSwaps, finalSwaps, extensionDays
        );

        // 7. Trả về kết quả
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

                // Thông tin tính toán
                .pricePerSwapOld(pricePerSwapOld)
                .pricePerSwapNew(pricePerSwapNew)
                .totalPaidForOldPackage(currentPackage.getPrice())
                .noRefund(BigDecimal.ZERO)
                .downgradePenaltyPercent(penaltyPercent.multiply(new BigDecimal(100)))
                .penaltySwaps(penaltySwaps)
                .finalRemainingSwaps(finalSwaps)

                // Sau khi hạ cấp
                .newStartDate(newStartDate)
                .newEndDate(newEndDate)
                .extensionDays(extensionDays)

                // Trạng thái
                .canDowngrade(true)
                .reason("Bạn đủ điều kiện để hạ cấp gói. Vui lòng xem kỹ cảnh báo trước khi xác nhận.")
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
            throw new AuthenticationException("Chỉ tài xế mới được phép hạ cấp gói dịch vụ.");
        }

        // 1. Kiểm tra điều kiện hạ cấp bằng phương thức calculate
        DowngradeCalculationResponse calculation = calculateDowngradeCost(newPackageId);

        if (!calculation.getCanDowngrade()) {
            throw new IllegalStateException(
                    "Không thể hạ cấp gói: " + calculation.getReason()
            );
        }

        // 2. Lấy gói đăng ký hiện tại
        DriverSubscription oldSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentDriver, LocalDate.now())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ nào đang hoạt động."));

        ServicePackage oldPackage = oldSubscription.getServicePackage();
        ServicePackage newPackage = servicePackageRepository.findById(newPackageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + newPackageId));

        // 3. Ghi log thông tin hạ cấp
        log.info("HẠ CẤP GÓI - Tài xế: {} | Gói cũ: {} ({} lượt, còn {} lượt) | Gói mới: {} (phạt {} lượt, còn lại {} lượt, gia hạn {} ngày)",
                currentDriver.getEmail(),
                oldPackage.getName(),
                oldPackage.getMaxSwaps(),
                oldSubscription.getRemainingSwaps(),
                newPackage.getName(),
                calculation.getPenaltySwaps(),
                calculation.getFinalRemainingSwaps(),
                calculation.getExtensionDays()
        );

        // 4. Hết hạn gói cũ
        oldSubscription.setStatus(DriverSubscription.Status.EXPIRED);
        oldSubscription.setEndDate(LocalDate.now());
        driverSubscriptionRepository.save(oldSubscription);

        log.info("Gói cũ {} đã hết hạn. {} lượt bị hủy (bao gồm {} lượt phạt).",
                oldSubscription.getId(),
                oldSubscription.getRemainingSwaps(),
                calculation.getPenaltySwaps()
        );

        // 5. Tạo gói dịch vụ mới
        DriverSubscription newSubscription = new DriverSubscription();
        newSubscription.setDriver(currentDriver);
        newSubscription.setServicePackage(newPackage);
        newSubscription.setStartDate(calculation.getNewStartDate());
        newSubscription.setEndDate(calculation.getNewEndDate());
        newSubscription.setStatus(DriverSubscription.Status.ACTIVE);
        newSubscription.setRemainingSwaps(calculation.getFinalRemainingSwaps()); // Số lượt sau khi trừ penalty

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(newSubscription);

        log.info("HẠ CẤP THÀNH CÔNG - Gói mới {} đã được tạo: {} lượt, hết hạn vào {} (được gia hạn thêm {} ngày).",
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
     * TÍNH TOÁN CHI PHÍ GIA HẠN GÓI (RENEWAL - SAME PACKAGE ONLY)
     *
     * CHỈ CHO PHÉP GIA HẠN CÙNG GÓI HIỆN TẠI!
     * Nếu muốn đổi gói khác → Dùng chức năng NÂNG CẤP hoặc HẠ CẤP
     *
     * CASE 1: EARLY RENEWAL (còn hạn)
     * - Stack swaps: totalSwaps = remainingSwaps + newMaxSwaps
     * - Stack duration: newEndDate = currentEndDate + newDuration
     * - Discount: 5% (khuyến khích renew sớm)
     *
     * CASE 2: LATE RENEWAL (hết hạn)
     * - Reset swaps: totalSwaps = newMaxSwaps (mất lượt cũ)
     * - Reset duration: newEndDate = today + newDuration
     * - No discount
     *
     * @param renewalPackageId ID của gói muốn gia hạn (PHẢI CÙNG GÓI HIỆN TẠI)
     * @return RenewalCalculationResponse
     */
    @Transactional(readOnly = true)
    public RenewalCalculationResponse calculateRenewalCost(Long renewalPackageId) {
        User currentDriver = authenticationService.getCurrentUser();

        if (currentDriver.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Chỉ tài xế mới có thể tính toán chi phí gia hạn gói dịch vụ.");
        }

        // 1. Lấy gói dịch vụ gần nhất của tài xế (dựa trên subscriptionId - gói được tạo sau cùng)
        List<DriverSubscription> allSubs = driverSubscriptionRepository.findByDriver_Id(currentDriver.getId());

        if (allSubs.isEmpty()) {
            throw new NotFoundException("Bạn chưa có gói dịch vụ nào. Vui lòng mua gói mới thay vì gia hạn.");
        }

        // Lấy subscription mới nhất theo ID
        DriverSubscription latestSub = allSubs.stream()
                .max((s1, s2) -> s1.getId().compareTo(s2.getId()))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin gói dịch vụ gần nhất."));

        // Kiểm tra nếu gói gần nhất đã bị hủy
        if (latestSub.getStatus() == DriverSubscription.Status.CANCELLED) {
            throw new IllegalArgumentException(
                    "Gói gần nhất của bạn đã bị hủy. Vui lòng mua gói mới thay vì gia hạn."
            );
        }

        // Chỉ cho phép gia hạn nếu gói gần nhất đang ở trạng thái ACTIVE hoặc EXPIRED
        if (latestSub.getStatus() != DriverSubscription.Status.ACTIVE
                && latestSub.getStatus() != DriverSubscription.Status.EXPIRED) {
            throw new IllegalArgumentException(
                    "Không thể gia hạn gói có trạng thái: " + latestSub.getStatus()
            );
        }

        DriverSubscription currentSub = latestSub;
        ServicePackage currentPackage = currentSub.getServicePackage();

        // 2. Lấy thông tin gói muốn gia hạn
        ServicePackage renewalPackage = servicePackageRepository.findById(renewalPackageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + renewalPackageId));

        // 2.1. Kiểm tra hợp lệ: chỉ cho phép gia hạn cùng gói hiện tại
        if (!currentPackage.getId().equals(renewalPackageId)) {
            throw new IllegalArgumentException(
                    "KHÔNG THỂ GIA HẠN! Bạn chỉ được phép gia hạn cùng gói hiện tại. " +
                            "Gói hiện tại: \"" + currentPackage.getName() + "\" (ID: " + currentPackage.getId() + "). " +
                            "Gói bạn chọn: \"" + renewalPackage.getName() + "\" (ID: " + renewalPackageId + "). " +
                            "Nếu muốn đổi gói khác, vui lòng sử dụng chức năng NÂNG CẤP hoặc HẠ CẤP."
            );
        }

        // 3. Xác định loại gia hạn: HẾT HẠN (LATE) hoặc SỚM (EARLY)
        LocalDate today = LocalDate.now();
        boolean isExpired = currentSub.getEndDate().isBefore(today);
        String renewalType = isExpired ? "HẾT HẠN" : "GIA HẠN SỚM";

        long daysRemaining = isExpired ? 0 : ChronoUnit.DAYS.between(today, currentSub.getEndDate());
        Integer remainingSwaps = currentSub.getRemainingSwaps();

        // 4. Luôn gia hạn cùng gói
        boolean isSamePackage = true;

        // 5. TÍNH TOÁN CHI PHÍ
        BigDecimal originalPrice = renewalPackage.getPrice();
        BigDecimal earlyDiscount = BigDecimal.ZERO;

        // 5.1. Giảm giá 5% nếu gia hạn sớm
        if (!isExpired) {
            earlyDiscount = originalPrice.multiply(new BigDecimal("0.05"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 5.2. Tổng giảm giá
        BigDecimal totalDiscount = earlyDiscount;
        BigDecimal finalPrice = originalPrice.subtract(totalDiscount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 6. TÍNH SAU KHI GIA HẠN
        Integer totalSwaps;
        LocalDate newStartDate;
        LocalDate newEndDate;
        Integer totalDuration;
        Integer stackedSwaps = 0;

        if (isExpired) {
            // GIA HẠN SAU KHI HẾT HẠN - Reset hoàn toàn
            totalSwaps = renewalPackage.getMaxSwaps();
            newStartDate = today;
            newEndDate = today.plusDays(renewalPackage.getDuration());
            totalDuration = renewalPackage.getDuration();
        } else {
            // GIA HẠN SỚM - Cộng dồn lượt swap & thời gian
            totalSwaps = remainingSwaps + renewalPackage.getMaxSwaps();
            stackedSwaps = remainingSwaps; // Số lượt được giữ lại
            newStartDate = today;
            newEndDate = currentSub.getEndDate().plusDays(renewalPackage.getDuration());
            totalDuration = (int) ChronoUnit.DAYS.between(today, newEndDate);
        }

        // 7. Tính giá mỗi lượt & mức tiết kiệm
        BigDecimal pricePerSwap = finalPrice.divide(new BigDecimal(renewalPackage.getMaxSwaps()), 2, RoundingMode.HALF_UP);
        BigDecimal savingsAmount = totalDiscount;

        // 8. Gợi ý cho người dùng
        String recommendation = generateRenewalRecommendation(
                currentPackage, renewalPackage, isExpired, isSamePackage,
                totalDiscount, stackedSwaps
        );

        String message = isExpired
                ? "Gói dịch vụ của bạn đã hết hạn. Hãy gia hạn ngay để tiếp tục sử dụng! (Chỉ được gia hạn cùng gói hiện tại)"
                : String.format("Bạn có thể gia hạn sớm để nhận ưu đãi! Còn %d ngày và %d lượt swap. (Chỉ được gia hạn cùng gói hiện tại)",
                daysRemaining, remainingSwaps);

        // 9. Trả về kết quả
        return RenewalCalculationResponse.builder()
                // Gói hiện tại
                .currentSubscriptionId(currentSub.getId())
                .currentPackageName(currentPackage.getName())
                .currentPackagePrice(currentPackage.getPrice())
                .currentMaxSwaps(currentPackage.getMaxSwaps())
                .remainingSwaps(remainingSwaps)
                .currentStartDate(currentSub.getStartDate())
                .currentEndDate(currentSub.getEndDate())
                .daysRemaining((int) daysRemaining)
                .isExpired(isExpired)

                // Gói gia hạn
                .renewalPackageId(renewalPackage.getId())
                .renewalPackageName(renewalPackage.getName())
                .renewalPackagePrice(renewalPackage.getPrice())
                .renewalMaxSwaps(renewalPackage.getMaxSwaps())
                .renewalDuration(renewalPackage.getDuration())

                // Giá & khuyến mãi
                .renewalType(renewalType)
                .isSamePackage(isSamePackage)
                .earlyRenewalDiscount(earlyDiscount)
                .samePackageDiscount(BigDecimal.ZERO)
                .totalDiscount(totalDiscount)
                .originalPrice(originalPrice)
                .finalPrice(finalPrice)

                // Sau gia hạn
                .totalSwapsAfterRenewal(totalSwaps)
                .newStartDate(newStartDate)
                .newEndDate(newEndDate)
                .totalDuration(totalDuration)
                .stackedSwaps(stackedSwaps)

                // Thông tin hiển thị
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
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài xế với ID: " + driverId));

        ServicePackage renewalPackage = servicePackageRepository.findById(renewalPackageId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + renewalPackageId));

        // Lấy gói đăng ký mới nhất của tài xế (dựa theo ID lớn nhất)
        List<DriverSubscription> allSubs = driverSubscriptionRepository.findByDriver_Id(driverId);
        DriverSubscription oldSubscription = allSubs.stream()
                .max((s1, s2) -> s1.getId().compareTo(s2.getId()))
                .orElse(null);

        // Kiểm tra nếu gói gần nhất bị HỦY
        if (oldSubscription != null && oldSubscription.getStatus() == DriverSubscription.Status.CANCELLED) {
            throw new IllegalArgumentException(
                    "Gói gần nhất của bạn đã bị hủy. Không thể gia hạn. Vui lòng mua gói mới."
            );
        }

        // Chỉ cho phép gia hạn nếu gói mới nhất đang ở trạng thái ACTIVE hoặc EXPIRED
        if (oldSubscription != null
                && oldSubscription.getStatus() != DriverSubscription.Status.ACTIVE
                && oldSubscription.getStatus() != DriverSubscription.Status.EXPIRED) {
            throw new IllegalArgumentException(
                    "Không thể gia hạn gói có trạng thái: " + oldSubscription.getStatus()
            );
        }

        // VALIDATION: Chỉ cho phép gia hạn CÙNG GÓI
        if (oldSubscription != null) {
            ServicePackage oldPackage = oldSubscription.getServicePackage();
            if (!oldPackage.getId().equals(renewalPackageId)) {
                throw new IllegalArgumentException(
                        "❌ KHÔNG THỂ GIA HẠN! Bạn chỉ được gia hạn cùng gói hiện tại. " +
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

            log.info("GIA HẠN GÓI - Tài xế: {} | Gói cũ: {} (hết hạn: {}, còn lại: {} lượt) | Gói mới: {}",
                    driver.getEmail(),
                    oldPackage.getName(),
                    isExpired,
                    oldSubscription.getRemainingSwaps(),
                    renewalPackage.getName()
            );

            if (!isExpired) {
                // GIA HẠN SỚM - cộng dồn lượt swap và thời hạn
                stackedSwaps = oldSubscription.getRemainingSwaps();
                newEndDate = oldSubscription.getEndDate().plusDays(renewalPackage.getDuration());
            } else {
                // GIA HẠN TRỄ - reset lại gói mới
                newEndDate = today.plusDays(renewalPackage.getDuration());
            }

            // Đánh dấu gói cũ là HẾT HẠN
            oldSubscription.setStatus(DriverSubscription.Status.EXPIRED);
            oldSubscription.setEndDate(today);
            driverSubscriptionRepository.save(oldSubscription);

            log.info("Gói cũ {} đã được đánh dấu hết hạn.", oldSubscription.getId());
        } else {
            // Lần đầu mua gói (chưa có subscription cũ)
            newEndDate = today.plusDays(renewalPackage.getDuration());
        }

        // Tạo gói đăng ký mới
        DriverSubscription newSubscription = new DriverSubscription();
        newSubscription.setDriver(driver);
        newSubscription.setServicePackage(renewalPackage);
        newSubscription.setStartDate(newStartDate);
        newSubscription.setEndDate(newEndDate);
        newSubscription.setStatus(DriverSubscription.Status.ACTIVE);

        // Cộng dồn lượt nếu gia hạn sớm
        Integer totalSwaps = renewalPackage.getMaxSwaps() + stackedSwaps;
        newSubscription.setRemainingSwaps(totalSwaps);

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(newSubscription);

        log.info("GIA HẠN THÀNH CÔNG - Gói mới {} được tạo: {} lượt (cộng dồn: {}), hết hạn ngày {}",
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
        } else {
            rec.append("Gia hạn sớm! ");
            if (stackedSwaps > 0) {
                rec.append(String.format("Bạn sẽ giữ được %d lượt chưa dùng + thêm %d lượt mới = %d lượt! ",
                        stackedSwaps, renewalPackage.getMaxSwaps(), stackedSwaps + renewalPackage.getMaxSwaps()));
            }

            if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
                rec.append(String.format("Tiết kiệm %,d VNĐ nhờ ưu đãi gia hạn sớm (5%%). ",
                        totalDiscount.intValue()));
            }
        }

        rec.append("Gia hạn gói đang dùng - Lựa chọn thông minh! ");

        return rec.toString();
    }
}