package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.response.UpgradeCalculationResponse;
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

    @Autowired
    private final EmailService emailService;

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
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đăng ký trình điều khiển có id:" + id));

        // Lưu thông tin trước khi xóa để gửi email
        User driver = subscription.getDriver();
        String adminName = currentUser.getFullName() != null ? currentUser.getFullName() : "Quản trị viên";

        // Log thông tin
        log.info("Quản trị viên {} đang xóa đăng ký {} cho trình điều khiển {}",
                currentUser.getEmail(),
                subscription.getId(),
                driver.getEmail());

        // Chuyển status thành CANCELLED
        subscription.setStatus(DriverSubscription.Status.CANCELLED);
        driverSubscriptionRepository.save(subscription);

        // Gửi email thông báo cho driver
        try {
            String reason = String.format(
                    "Gói dịch vụ '%s' của bạn đã bị hủy bởi quản trị viên hệ thống. " +
                            "Nếu bạn cho rằng đây là một nhầm lẫn hoặc cần thêm thông tin, " +
                            "vui lòng liên hệ với bộ phận hỗ trợ khách hàng của chúng tôi.",
                    subscription.getServicePackage().getName()
            );

            emailService.sendSubscriptionDeletedEmail(driver, subscription, adminName, reason);
            log.info("Subscription deletion email sent successfully to driver: {}", driver.getEmail());
        } catch (Exception e) {
            log.error("Failed to send subscription deletion email to driver {}: {}",
                    driver.getEmail(), e.getMessage());
            // Không throw exception để không ảnh hưởng đến quá trình xóa subscription
        }
    }

    // ========================================
    // NÂNG CẤP GÓI (UPGRADE PACKAGE)
    // ========================================

    /**
     * TÍNH TOÁN CHI PHÍ NÂNG CẤP GÓI - MÔ HÌNH TELCO (ĐƠN GIẢN NHẤT)
     *
     * BUSINESS RULES:
     * 1. HỦY gói cũ ngay lập tức (CANCELLED)
     * 2. KÍCH HOẠT gói mới FULL 100%
     * 3. THANH TOÁN = Giá FULL gói mới
     *
     * VÍ DỤ:
     * - Gói cũ: Basic (20 lượt = 400,000đ, còn 15 lượt, 20 ngày)
     * - Gói mới: Premium (50 lượt = 800,000đ, 60 ngày)
     *
     * KẾT QUẢ:
     * - GÓI CŨ: Bị HỦY ngay → MẤT 15 lượt + 20 ngày
     * - GÓI MỚI: FULL 50 lượt + 60 ngày MỚI
     * - THANH TOÁN: 800,000đ (FULL giá gói mới)
     *
     * → GIỐNG MÔ HÌNH VIETTEL/VINAPHONE ĐỔI GÓI DATA
     * → KHÔNG hoàn tiền, KHÔNG bonus, KHÔNG phí phạt
     * → CỰC KỲ ĐƠN GIẢN, NGĂN 100% LẠM DỤNG
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
        Integer remainingSwaps = currentSub.getRemainingSwaps();

        LocalDate today = LocalDate.now();
        long daysUsed = ChronoUnit.DAYS.between(currentSub.getStartDate(), today);
        long daysRemaining = ChronoUnit.DAYS.between(today, currentSub.getEndDate());

        // ========================================
        // 5. TÍNH TOÁN THANH TOÁN - SIÊU ĐƠN GIẢN!
        // ========================================

        // PHƯƠNG ÁN A: Trả FULL giá gói mới
        BigDecimal paymentRequired = newPackage.getPrice();

        // Ước tính giá trị mất mát (chỉ để hiển thị cho user)
        long totalDays = ChronoUnit.DAYS.between(currentSub.getStartDate(), currentSub.getEndDate());
        BigDecimal estimatedLostValue = totalDays > 0
                ? currentPackage.getPrice()
                .multiply(new BigDecimal(daysRemaining))
                .divide(new BigDecimal(totalDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 6. Thông tin sau nâng cấp
        LocalDate newStartDate = LocalDate.now();
        LocalDate newEndDate = newStartDate.plusDays(newPackage.getDuration());

        // 7. Cảnh báo QUAN TRỌNG
        String warning = String.format(
                "CẢNH BÁO QUAN TRỌNG - VUI LÒNG ĐỌC KỸ:\n\n" +
                        "KHI NÂNG CẤP, BẠN SẼ:\n" +
                        "MẤT NGAY: %d lượt đổi pin còn lại\n" +
                        "MẤT NGAY: %d ngày thời hạn còn lại  \n" +
                        "GÓI CŨ: Bị HỦY hoàn toàn (CANCELLED)\n\n" +
                        "SAU NÂNG CẤP, BẠN NHẬN:\n" +
                        "GÓI MỚI: %d lượt FULL (không bonus)\n" +
                        "THỜI HẠN: %d ngày MỚI (bắt đầu từ hôm nay)\n\n" +
                        "THANH TOÁN:\n" +
                        "• Giá: %,d VNĐ (FULL giá gói mới)\n" +
                        "• Không hoàn lại phần gói cũ\n\n" +
                        "LƯU Ý: Nếu gói cũ còn nhiều, hãy sử dụng thêm trước khi nâng cấp!",
                remainingSwaps,
                daysRemaining,
                newPackage.getMaxSwaps(),
                newPackage.getDuration(),
                newPackage.getPrice().intValue()
        );

        // 8. Phân tích
        String analysis = generateTelcoStyleAnalysis(
                currentPackage, newPackage,
                remainingSwaps, daysRemaining, totalDays,
                estimatedLostValue, paymentRequired
        );

        // 9. Build response
        return UpgradeCalculationResponse.builder()
                // Gói hiện tại
                .currentSubscriptionId(currentSub.getId())
                .currentPackageName(currentPackage.getName())
                .currentPackagePrice(currentPackage.getPrice())
                .currentMaxSwaps(currentPackage.getMaxSwaps())
                .usedSwaps(currentPackage.getMaxSwaps() - remainingSwaps)
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

                // Tính toán (ĐƠN GIẢN!)
                .refundValue(BigDecimal.ZERO) // KHÔNG HOÀN
                .upgradeFee(BigDecimal.ZERO) // KHÔNG PHÍ
                .totalPaymentRequired(paymentRequired) // FULL PRICE
                .estimatedLostValue(estimatedLostValue) // CHỈ ĐỂ HIỂN THỊ

                // Sau nâng cấp
                .totalSwapsAfterUpgrade(newPackage.getMaxSwaps()) // FULL
                .newStartDate(newStartDate)
                .newEndDate(newEndDate)

                // Thông báo
                .canUpgrade(true)
                .message("Bạn có thể nâng cấp. Gói cũ sẽ BỊ HỦY, gói mới kích hoạt FULL.")
                .warning(warning)
                .recommendation(analysis)
                .build();
    }

    /**
     * PHÂN TÍCH THEO MÔ HÌNH TELCO
     */
    private String generateTelcoStyleAnalysis(
            ServicePackage currentPackage,
            ServicePackage newPackage,
            Integer remainingSwaps,
            long daysRemaining,
            long totalDays,
            BigDecimal estimatedLostValue,
            BigDecimal paymentRequired
    ) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("PHÂN TÍCH CHI TIẾT:\n\n");

        // 1. Thông tin mất mát
        analysis.append(String.format(
                "BẠN SẼ MẤT:\n" +
                        "   • %d lượt đổi pin\n" +
                        "   • %d ngày thời hạn\n" +
                        "   • Giá trị ước tính: ~%,d VNĐ\n" +
                        "   • GÓI CŨ: Bị hủy hoàn toàn\n\n",
                remainingSwaps,
                daysRemaining,
                estimatedLostValue.intValue()
        ));

        // 2. Thông tin nhận được
        analysis.append(String.format(
                "BẠN SẼ NHẬN:\n" +
                        "   • %d lượt đổi FULL (100%%, không bonus)\n" +
                        "   • %d ngày MỚI (bắt đầu từ hôm nay)\n" +
                        "   • Giá trị: %,d VNĐ\n\n",
                newPackage.getMaxSwaps(),
                newPackage.getDuration(),
                newPackage.getPrice().intValue()
        ));

        // 3. Thanh toán
        analysis.append(String.format(
                "THANH TOÁN:\n" +
                        "   • TỔNG: %,d VNĐ (FULL giá gói mới)\n" +
                        "   • Không hoàn lại: 0 VNĐ\n" +
                        "   • Không phí phạt: 0 VNĐ\n\n",
                paymentRequired.intValue()
        ));

        // 4. Gợi ý
        double remainPercent = totalDays > 0 ? (daysRemaining * 100.0) / totalDays : 0;

        if (remainPercent > 70) {
            analysis.append(
                    "CẢNH BÁO:\n" +
                            String.format("   • Gói cũ còn %.0f%% (%d/%d ngày)\n",
                                    remainPercent, daysRemaining, totalDays) +
                            String.format("   • Bạn sẽ MẤT TRẮNG ~%,d VNĐ\n", estimatedLostValue.intValue()) +
                            "   • GỢI Ý: Hãy dùng thêm gói cũ trước!\n\n"
            );
        } else if (remainPercent > 30) {
            analysis.append(
                    "CÂN NHẮC:\n" +
                            String.format("   • Gói cũ còn %.0f%%\n", remainPercent) +
                            String.format("   • Mất ~%,d VNĐ nếu nâng ngay\n\n", estimatedLostValue.intValue())
            );
        } else {
            analysis.append("THỜI ĐIỂM TỐT: Gói cũ sắp hết!\n\n");
        }

        // 5. So sánh Telco
        analysis.append(
                "📱 TƯƠNG TỰ VIETTEL/VINA:\n" +
                        "   • Gói cũ → MẤT NGAY\n" +
                        "   • Gói mới → FULL 100%\n" +
                        "   • Trả → FULL giá mới\n"
        );

        return analysis.toString();
    }

    /**
     * XỬ LÝ NÂNG CẤP GÓI SAU KHI THANH TOÁN THÀNH CÔNG (TELCO MODEL)
     *
     * MÔ HÌNH TELCO - PHƯƠNG ÁN A (ĐƠN GIẢN NHẤT):
     * 1. HỦY gói cũ ngay lập tức (Status = EXPIRED, mất hết lượt và ngày còn lại)
     * 2. KÍCH HOẠT gói mới với FULL capacity:
     *    - Swaps = 100% (newPackage.getMaxSwaps())
     *    - Duration = 100% (newPackage.getDuration())
     *    - StartDate = TODAY
     * 3. KHÔNG có bonus, KHÔNG có refund
     *
     * Giống như Viettel/Vinaphone đổi gói data:
     * - Data cũ: MẤT HẾT
     * - Data mới: FULL 100%
     * - Thanh toán: GIÁ ĐẦY ĐỦ
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

        // Ghi log thông tin nâng cấp TELCO STYLE
        log.info("========== NÂNG CẤP GÓI (TELCO MODEL) ==========");
        log.info("Tài xế: {}", driver.getEmail());
        log.info("Gói CỦ: {} - {} lượt - Còn lại: {} lượt - Status: {} → EXPIRED (HỦY TOÀN BỘ)",
                oldPackage.getName(),
                oldPackage.getMaxSwaps(),
                oldSubscription.getRemainingSwaps(),
                oldSubscription.getStatus()
        );
        log.info("Gói MỚI: {} - {} lượt FULL - {} VNĐ - {} ngày",
                newPackage.getName(),
                newPackage.getMaxSwaps(),
                newPackage.getPrice(),
                newPackage.getDuration()
        );

        // HỦY gói cũ (TELCO: mất hết)
        oldSubscription.setStatus(DriverSubscription.Status.EXPIRED);
        oldSubscription.setEndDate(LocalDate.now()); // Kết thúc ngay hôm nay
        driverSubscriptionRepository.save(oldSubscription);

        log.info("Gói cũ ID={} đã HỦY. {} lượt bị MẤT TRẮNG (TELCO model).",
                oldSubscription.getId(),
                oldSubscription.getRemainingSwaps()
        );

        // Tạo gói đăng ký mới
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(newPackage.getDuration());

        DriverSubscription newSubscription = new DriverSubscription();
        newSubscription.setDriver(driver);
        newSubscription.setServicePackage(newPackage);
        newSubscription.setStartDate(startDate);
        newSubscription.setEndDate(endDate);
        newSubscription.setStatus(DriverSubscription.Status.ACTIVE);
        newSubscription.setRemainingSwaps(newPackage.getMaxSwaps()); // FULL 100% - KHÔNG BONUS

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(newSubscription);

        log.info("NÂNG CẤP THÀNH CÔNG - Gói mới ID={}: {} lượt FULL (100%), hết hạn {}",
                savedSubscription.getId(),
                savedSubscription.getRemainingSwaps(),
                savedSubscription.getEndDate()
        );
        log.info("================================================");

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