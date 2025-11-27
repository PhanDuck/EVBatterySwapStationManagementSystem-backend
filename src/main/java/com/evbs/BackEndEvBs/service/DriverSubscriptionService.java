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

        // Populate names
        populateSubscriptionNames(savedSubscription);

        return savedSubscription;
    }

    @Transactional(readOnly = true)
    public List<DriverSubscription> getAllSubscriptions() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Quyền truy cập bị từ chối. Yêu cầu vai trò quản trị viên.");
        }
        List<DriverSubscription> subscriptions = driverSubscriptionRepository.findAll();
        populateSubscriptionsNames(subscriptions);
        return subscriptions;
    }

    @Transactional(readOnly = true)
    public List<DriverSubscription> getMySubscriptions() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Chỉ có tài xế mới có thể xem đăng ký của họ");
        }
        List<DriverSubscription> subscriptions = driverSubscriptionRepository.findByDriver_Id(currentUser.getId());
        populateSubscriptionsNames(subscriptions);
        return subscriptions;
    }

    @Transactional
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
     * TÍNH TOÁN CHI PHÍ NÂNG CẤP GÓI
     *
     * CÔNG THỨC MỚI:
     * 1. Giá trị hoàn lại = (Lượt chưa dùng) × (Giá gói cũ / Tổng lượt gói cũ)
     * 2. Số tiền cần trả = Giá gói mới - Giá trị hoàn lại
     *
     * VÍ DỤ:
     * - Gói cũ: 20 lượt = 400,000đ (đã dùng 5, còn 15)
     * - Gói mới: 50 lượt = 800,000đ
     * - Giá trị hoàn lại = 15 × (400,000 / 20) = 15 × 20,000 = 300,000đ
     * - Tổng tiền = 800,000 - 300,000 = 500,000đ
     *
     * BUSINESS RULES:
     * 1. HỦY gói cũ ngay lập tức (CANCELLED)
     * 2. KÍCH HOẠT gói mới FULL 100%
     * 3. THANH TOÁN = Giá gói mới - Giá trị hoàn lại từ gói cũ
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
        // 5. TÍNH TOÁN THANH TOÁN THEO CÔNG THỨC MỚI
        // ========================================

        // Bước 1: Tính giá trị hoàn lại = (Lượt chưa dùng) × (Giá gói cũ / Tổng lượt gói cũ)
        BigDecimal pricePerSwapOld = currentPackage.getPrice()
                .divide(new BigDecimal(currentPackage.getMaxSwaps()), 0, RoundingMode.HALF_UP);

        BigDecimal refundValue = pricePerSwapOld
                .multiply(new BigDecimal(remainingSwaps))
                .setScale(0, RoundingMode.HALF_UP);

        // VALIDATION 1: Kiểm tra giá trị hoàn > giá trị gói mới
        if (refundValue.compareTo(newPackage.getPrice()) > 0) {
            throw new IllegalArgumentException(
                    "Không thể nâng cấp! Giá trị hoàn lại (" + String.format("%,d", refundValue.longValue()) + 
                    " VNĐ) lớn hơn giá gói mới (" + String.format("%,d", newPackage.getPrice().longValue()) + 
                    " VNĐ). Vui lòng chọn gói cao hơn hoặc dùng hết lượt trước."
            );
        }

        // Bước 2: Số tiền cần trả = Giá gói mới - Giá trị hoàn lại
        BigDecimal paymentRequired = newPackage.getPrice()
                .subtract(refundValue)
                .max(BigDecimal.ZERO)  // Đảm bảo không âm
                .setScale(0, RoundingMode.HALF_UP);

        // VALIDATION 2: Kiểm tra số tiền thanh toán phải >= 1,000 VNĐ (yêu cầu của MoMo)
        BigDecimal minimumPayment = new BigDecimal("1000");
        if (paymentRequired.compareTo(minimumPayment) < 0 && paymentRequired.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException(
                    "Không thể nâng cấp! Số tiền thanh toán (" + String.format("%,d", paymentRequired.longValue()) + 
                    " VNĐ) phải từ 1,000 VNĐ trở lên (yêu cầu MoMo). Vui lòng chọn gói cao hơn hoặc dùng hết lượt trước."
            );
        }

        // Ước tính giá trị mất mát (về ngày còn lại - chỉ để hiển thị)
        long totalDays = ChronoUnit.DAYS.between(currentSub.getStartDate(), currentSub.getEndDate());
        BigDecimal estimatedLostValue = totalDays > 0
                ? currentPackage.getPrice()
                .multiply(new BigDecimal(daysRemaining))
                .divide(new BigDecimal(totalDays), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 6. Thông tin sau nâng cấp
        LocalDate newStartDate = LocalDate.now();
        LocalDate newEndDate = newStartDate.plusDays(newPackage.getDuration());

        // 7. Cảnh báo QUAN TRỌNG
        String warning = String.format(
                "CẢNH BÁO QUAN TRỌNG - VUI LÒNG ĐỌC KỸ:\n\n" +
                        "KHI NÂNG CẤP:\n" +
                        "• GÓI CŨ sẽ bị HỦY hoàn toàn\n" +
                        "• Bạn sẽ được HOÀN LẠI giá trị của %d lượt chưa dùng = %,.0f VNĐ\n" +
                        "• GÓI MỚI sẽ được kích hoạt NGAY với FULL %d lượt\n" +
                        "• Số tiền cần thanh toán: %,.0f VNĐ (= Giá gói mới - Giá trị hoàn lại)\n\n" +
                        "LƯU Ý: Hãy suy nghĩ thật kỹ trước khi nâng cấp!",
                remainingSwaps,
                refundValue,
                newPackage.getMaxSwaps(),
                paymentRequired
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

                // Tính toán (CÓ HOÀN LẠI GIÁ TRỊ LƯỢT CHƯA DÙNG)
                .pricePerSwapOld(pricePerSwapOld) // Giá/lượt gói cũ
                .refundValue(refundValue) // Giá trị hoàn lại
                .upgradeFee(BigDecimal.ZERO) // KHÔNG PHÍ
                .totalPaymentRequired(paymentRequired) // Giá gói mới - Giá trị hoàn lại
                .estimatedLostValue(estimatedLostValue) // Giá trị ngày còn lại bị mất

                // Sau nâng cấp
                .totalSwapsAfterUpgrade(newPackage.getMaxSwaps()) // FULL
                .newStartDate(newStartDate)
                .newEndDate(newEndDate)

                // Thông báo
                .canUpgrade(true)
                .message("Bạn có thể nâng cấp. Gói cũ sẽ BỊ HỦY, nhưng bạn sẽ được hoàn lại giá trị lượt chưa dùng. Gói mới sẽ kích hoạt ngay lập tức.")
                .warning(warning)
                .recommendation(analysis)
                .build();
    }

    /**
     * PHÂN TÍCH CHI PHÍ NÂNG CẤP
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

        // 1. Tính giá trị lượt chưa dùng
        BigDecimal pricePerSwap = currentPackage.getPrice()
                .divide(new BigDecimal(currentPackage.getMaxSwaps()), 2, RoundingMode.HALF_UP);
        BigDecimal refundValue = pricePerSwap
                .multiply(new BigDecimal(remainingSwaps))
                .setScale(2, RoundingMode.HALF_UP);

        analysis.append(String.format(
                "Thanh toán:\n" +
                        "Giá mới:     %,.0f đ\n\n" +
                        "Hoàn lại:    %,.0f đ\n\n" +
                        "Cần trả:     %,.0f đ\n\n" +
                        "Hãy suy nghĩ thật kĩ trước khi nâng cấp !\n\n",
                newPackage.getPrice(),
                refundValue,
                paymentRequired
        ));
        return analysis.toString();
    }

    /**
     * XỬ LÝ NÂNG CẤP GÓI SAU KHI THANH TOÁN THÀNH CÔNG
     *
     * LOGIC:
     * 1. HỦY gói cũ ngay lập tức (mất hết lượt và ngày còn lại)
     * 2. User đã được HOÀN LẠI giá trị lượt chưa dùng qua thanh toán
     * 3. KÍCH HOẠT gói mới với FULL capacity
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
        log.info("========== NÂNG CÁP GÓI (CÓ HOÀN LẠI) ==========");
        log.info("Tài xế: {}", driver.getEmail());
        log.info("Gói CỦ: {} - {} lượt - Còn lại: {} lượt - Status: {} → EXPIRED (HỦY)",
                oldPackage.getName(),
                oldPackage.getMaxSwaps(),
                oldSubscription.getRemainingSwaps(),
                oldSubscription.getStatus()
        );
        log.info("Giá trị hoàn lại: {} lượt × ({} VNĐ / {} lượt) = {} VNĐ (đã trừ vào thanh toán)",
                oldSubscription.getRemainingSwaps(),
                oldPackage.getPrice(),
                oldPackage.getMaxSwaps(),
                oldPackage.getPrice()
                        .divide(new BigDecimal(oldPackage.getMaxSwaps()), 2, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(oldSubscription.getRemainingSwaps()))
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

        log.info("Gói cũ ID={} đã HỦY. {} lượt đã được HOÀN LẠI giá trị qua thanh toán.",
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

        // Populate names
        populateSubscriptionNames(savedSubscription);

        return savedSubscription;
    }

    // ========================================
    // GIA HẠN GÓI (RENEWAL/EXTEND)
    // ========================================

    /**
     * TÍNH TOÁN CHI PHÍ GIA HẠN GÓI (RENEWAL - SAME PACKAGE ONLY)
     *
     * CHỈ CHO PHÉP GIA HẠN CÙNG GÓI HIỆN TẠI!
     * CHỈ CHO PHÉP GIA HẠN GÓI CÒN HIỆU LỰC (ACTIVE)!
     *
     * Nếu muốn đổi gói khác → Dùng chức năng NÂNG CẤP
     * Nếu gói đã HẾT HẠN → Phải MUA GÓI MỚI (không được renewal)
     *
     * EARLY RENEWAL (gia hạn sớm - gói còn hiệu lực):
     * - Cộng dồn swaps: totalSwaps = remainingSwaps + newMaxSwaps
     * - Cộng dồn duration: newEndDate = currentEndDate + newDuration
     * - Giảm giá: 5% (khuyến khích gia hạn sớm)
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

        // Kiểm tra nếu gói gần nhất đã hết hạn
        if (latestSub.getStatus() == DriverSubscription.Status.EXPIRED) {
            throw new IllegalArgumentException(
                    "Gói của bạn đã HẾT HẠN. Không thể gia hạn gói đã hết hạn. " +
                            "Vui lòng MUA GÓI MỚI thay vì gia hạn. " +
                            "(Gia hạn chỉ dành cho gói CÒN HIỆU LỰC)"
            );
        }

        // CHỈ cho phép gia hạn nếu gói đang ACTIVE (còn hiệu lực)
        if (latestSub.getStatus() != DriverSubscription.Status.ACTIVE) {
            throw new IllegalArgumentException(
                    "Không thể gia hạn gói có trạng thái: " + latestSub.getStatus() +
                            ". Chỉ có thể gia hạn gói đang HOẠT ĐỘNG (ACTIVE)."
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
                            "Nếu muốn đổi gói khác, vui lòng sử dụng chức năng NÂNG CẤP gói."
            );
        }

        // 3. Gói ACTIVE → Chỉ có EARLY RENEWAL (gia hạn sớm)
        LocalDate today = LocalDate.now();
        boolean isExpired = false; // Luôn false vì đã check ACTIVE ở trên
        String renewalType = "GIA HẠN SỚM";

        long daysRemaining = ChronoUnit.DAYS.between(today, currentSub.getEndDate());
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

        // 6. TÍNH SAU KHI GIA HẠN (CHỈ CÓ EARLY RENEWAL - CỘNG DỒN)
        // Cộng dồn lượt swap & thời gian
        Integer stackedSwaps = remainingSwaps; // Số lượt được giữ lại
        Integer totalSwaps = remainingSwaps + renewalPackage.getMaxSwaps();
        LocalDate newStartDate = today;
        LocalDate newEndDate = currentSub.getEndDate().plusDays(renewalPackage.getDuration());
        Integer totalDuration = (int) ChronoUnit.DAYS.between(today, newEndDate);

        // 7. Tính giá mỗi lượt & mức tiết kiệm
        BigDecimal pricePerSwap = finalPrice.divide(new BigDecimal(renewalPackage.getMaxSwaps()), 2, RoundingMode.HALF_UP);
        BigDecimal savingsAmount = totalDiscount;

        // 8. Gợi ý cho người dùng
        String recommendation = generateRenewalRecommendation(
                currentPackage, renewalPackage, isExpired, isSamePackage,
                totalDiscount, stackedSwaps
        );

        String message = String.format(
                "Bạn có thể gia hạn sớm để nhận ưu đãi! Còn %d ngày và %d lượt swap. " +
                        "(Chỉ được gia hạn cùng gói hiện tại. Gói đã hết hạn không thể gia hạn - phải mua gói mới)",
                daysRemaining, remainingSwaps
        );

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

        // Kiểm tra nếu gói gần nhất đã HẾT HẠN
        if (oldSubscription != null && oldSubscription.getStatus() == DriverSubscription.Status.EXPIRED) {
            throw new IllegalArgumentException(
                    "Gói của bạn đã HẾT HẠN. Không thể gia hạn gói đã hết hạn. " +
                            "Vui lòng MUA GÓI MỚI thay vì gia hạn. " +
                            "(Gia hạn chỉ dành cho gói CÒN HIỆU LỰC)"
            );
        }

        // CHỈ cho phép gia hạn nếu gói đang ACTIVE (còn hiệu lực)
        if (oldSubscription != null && oldSubscription.getStatus() != DriverSubscription.Status.ACTIVE) {
            throw new IllegalArgumentException(
                    "Không thể gia hạn gói có trạng thái: " + oldSubscription.getStatus() +
                            ". Chỉ có thể gia hạn gói đang HOẠT ĐỘNG (ACTIVE)."
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
                                "Nếu muốn đổi gói khác, vui lòng sử dụng chức năng NÂNG CẤP gói."
                );
            }
        }

        LocalDate today = LocalDate.now();
        LocalDate newStartDate = today;
        LocalDate newEndDate;

        Integer stackedSwaps = 0;

        if (oldSubscription != null) {
            ServicePackage oldPackage = oldSubscription.getServicePackage();

            log.info("GIA HẠN SỚM - Tài xế: {} | Gói cũ: {} (còn lại: {} lượt, {} ngày) | Gói mới: {}",
                    driver.getEmail(),
                    oldPackage.getName(),
                    oldSubscription.getRemainingSwaps(),
                    ChronoUnit.DAYS.between(today, oldSubscription.getEndDate()),
                    renewalPackage.getName()
            );

            // GIA HẠN SỚM - CỘNG DỒN lượt swap và thời hạn
            stackedSwaps = oldSubscription.getRemainingSwaps();
            newEndDate = oldSubscription.getEndDate().plusDays(renewalPackage.getDuration());

            // Đánh dấu gói cũ là HẾT HẠN
            oldSubscription.setStatus(DriverSubscription.Status.EXPIRED);
            oldSubscription.setEndDate(today);
            driverSubscriptionRepository.save(oldSubscription);

            log.info("Gói cũ {} đã được đánh dấu hết hạn. Lượt chưa dùng được CỘNG DỒN vào gói mới.", oldSubscription.getId());
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

        // Populate names
        populateSubscriptionNames(savedSubscription);
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

        rec.append("Gia hạn sớm - Lựa chọn thông minh! ");

        if (stackedSwaps > 0) {
            rec.append(String.format("Bạn sẽ giữ được %d lượt chưa dùng + thêm %d lượt mới = %d lượt! ",
                    stackedSwaps, renewalPackage.getMaxSwaps(), stackedSwaps + renewalPackage.getMaxSwaps()));
        }

        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            rec.append(String.format("Tiết kiệm %,d VNĐ nhờ ưu đãi gia hạn sớm (5%%). ",
                    totalDiscount.intValue()));
        }

        rec.append("Cộng dồn cả lượt và thời gian");

        return rec.toString();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Populate driverName và packageName cho một subscription
     */
    private void populateSubscriptionNames(DriverSubscription subscription) {
        if (subscription == null) return;

        if (subscription.getDriver() != null) {
            subscription.setDriverName(subscription.getDriver().getFullName());
        }
        if (subscription.getServicePackage() != null) {
            subscription.setPackageName(subscription.getServicePackage().getName());
        }
    }

    /**
     * Populate driverName và packageName cho danh sách subscriptions
     */
    private void populateSubscriptionsNames(List<DriverSubscription> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) return;

        subscriptions.forEach(this::populateSubscriptionNames);
    }
}