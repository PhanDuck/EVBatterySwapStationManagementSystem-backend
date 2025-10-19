package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.DriverSubscriptionRequest;
import com.evbs.BackEndEvBs.repository.DriverSubscriptionRepository;
import com.evbs.BackEndEvBs.repository.ServicePackageRepository;
import com.evbs.BackEndEvBs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    /**
     * ❌ DEPRECATED - Không dùng nữa
     * Dùng createSubscriptionAfterPayment() sau khi thanh toán thành công
     */
    @Deprecated
    @Transactional
    public DriverSubscription createSubscription(DriverSubscriptionRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can create subscriptions");
        }

        ServicePackage servicePackage = servicePackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new NotFoundException("Service package not found with id: " + request.getPackageId()));

        // Kiểm tra driver đã có subscription active chưa
        driverSubscriptionRepository.findActiveSubscriptionByDriver(currentUser, LocalDate.now())
            .ifPresent(existing -> {
                throw new AuthenticationException(
                    "❌ Bạn đã có gói dịch vụ ACTIVE! " +
                    "Gói hiện tại: " + existing.getServicePackage().getName() + " " +
                    "(còn " + existing.getRemainingSwaps() + " lượt swap). " +
                    "Vui lòng đợi hết hạn hoặc hủy gói cũ trước khi mua gói mới."
                );
            });

        // Tự động lấy ngày hiện tại
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(servicePackage.getDuration());

        DriverSubscription subscription = new DriverSubscription();
        subscription.setDriver(currentUser);
        subscription.setServicePackage(servicePackage);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(DriverSubscription.Status.ACTIVE);
        subscription.setRemainingSwaps(servicePackage.getMaxSwaps());

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(subscription);
        
        log.info("📦 Subscription created for driver {}: Package {} ({} swaps, {} VND)", 
                 currentUser.getEmail(), 
                 servicePackage.getName(),
                 servicePackage.getMaxSwaps(),
                 servicePackage.getPrice());

        return savedSubscription;
    }

    /**
     * ✅ TẠO SUBSCRIPTION SAU KHI THANH TOÁN THÀNH CÔNG
     * Được gọi từ MoMoService sau khi verify payment
     * 
     * BUSINESS RULES:
     * - ❌ Có gói ACTIVE + còn lượt swap → KHÔNG cho mua gói khác
     * - ✅ Có gói ACTIVE + hết lượt swap (remainingSwaps = 0) → CHO PHÉP mua gói khác
     * - ✅ Có gói EXPIRED → CHO PHÉP mua gói mới
     * - ✅ Không có gói → CHO PHÉP mua gói
     * 
     * WORKFLOW:
     * 1. Driver chọn packageId → Tạo MoMo payment URL
     * 2. Driver thanh toán MoMo
     * 3. MoMo callback → Gọi method này
     * 4. Kiểm tra gói hiện tại
     * 5. Nếu có gói active + còn lượt → reject
     * 6. Nếu hết lượt hoặc hết hạn → expire gói cũ, tạo gói mới
     */
    @Transactional
    public DriverSubscription createSubscriptionAfterPayment(Long packageId) {
        User currentUser = authenticationService.getCurrentUser();

        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("Service package not found with id: " + packageId));

        // Kiểm tra driver có subscription active không
        var activeSubscriptionOpt = driverSubscriptionRepository.findActiveSubscriptionByDriver(currentUser, LocalDate.now());
        
        if (activeSubscriptionOpt.isPresent()) {
            DriverSubscription existingSub = activeSubscriptionOpt.get();
            
            // ❌ CÒN LƯỢT SWAP → KHÔNG CHO MUA GÓI KHÁC
            if (existingSub.getRemainingSwaps() > 0) {
                throw new AuthenticationException(
                    "❌ Bạn đã có gói dịch vụ ACTIVE và còn lượt swap! " +
                    "Gói hiện tại: " + existingSub.getServicePackage().getName() + " " +
                    "(còn " + existingSub.getRemainingSwaps() + " lượt swap, " +
                    "hết hạn: " + existingSub.getEndDate() + "). " +
                    "Vui lòng sử dụng hết lượt swap hiện tại trước khi mua gói mới."
                );
            }
            
            // ✅ HẾT LƯỢT SWAP (remainingSwaps = 0) → CHO PHÉP MUA GÓI MỚI
            log.info("🔄 Driver {} has active subscription but 0 remaining swaps. Expiring old subscription...", 
                     currentUser.getEmail());
            
            // Expire gói cũ
            existingSub.setStatus(DriverSubscription.Status.EXPIRED);
            driverSubscriptionRepository.save(existingSub);
            
            log.info("✅ Old subscription {} expired (was active but had 0 swaps)", existingSub.getId());
        }

        // ✅ TẠO GÓI MỚI (vì không có gói active hoặc gói cũ đã hết lượt)
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(servicePackage.getDuration());

        DriverSubscription subscription = new DriverSubscription();
        subscription.setDriver(currentUser);
        subscription.setServicePackage(servicePackage);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(DriverSubscription.Status.ACTIVE); // ✅ ACTIVE ngay vì đã thanh toán rồi
        subscription.setRemainingSwaps(servicePackage.getMaxSwaps());

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(subscription);
        
        log.info("✅ Subscription created after payment: Driver {} → Package {} ({} swaps, {} VND)", 
                 currentUser.getEmail(), 
                 servicePackage.getName(),
                 servicePackage.getMaxSwaps(),
                 servicePackage.getPrice());

        return savedSubscription;
    }

    /**
     * ✅ TẠO SUBSCRIPTION SAU KHI THANH TOÁN (với driverId)
     * 
     * Overload method dùng cho payment gateway callback (MoMo)
     * khi không có authentication token
     * 
     * @param packageId ID của package
     * @param driverId ID của driver (lấy từ extraData)
     * @return DriverSubscription đã tạo
     */
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
            
            // ❌ CÒN LƯỢT SWAP → KHÔNG CHO MUA GÓI KHÁC
            if (existingSub.getRemainingSwaps() > 0) {
                throw new AuthenticationException(
                    "❌ Driver đã có gói dịch vụ ACTIVE và còn lượt swap! " +
                    "Gói hiện tại: " + existingSub.getServicePackage().getName() + " " +
                    "(còn " + existingSub.getRemainingSwaps() + " lượt swap, " +
                    "hết hạn: " + existingSub.getEndDate() + "). "
                );
            }
            
            // ✅ HẾT LƯỢT SWAP (remainingSwaps = 0) → CHO PHÉP MUA GÓI MỚI
            log.info("🔄 Driver {} has active subscription but 0 swaps remaining. Expiring old subscription...", 
                     driver.getEmail());
            
            // Expire gói cũ
            existingSub.setStatus(DriverSubscription.Status.EXPIRED);
            driverSubscriptionRepository.save(existingSub);
            
            log.info("✅ Old subscription {} expired (was active but had 0 swaps)", existingSub.getId());
        }

        // ✅ TẠO GÓI MỚI (vì không có gói active hoặc gói cũ đã hết lượt)
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
        
        log.info("✅ Subscription created after payment (callback): Driver {} → Package {} ({} swaps, {} VND)", 
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

    @Transactional
    public DriverSubscription updateSubscription(Long id, DriverSubscriptionRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        DriverSubscription existingSubscription = driverSubscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver subscription not found with id: " + id));

        if (request.getPackageId() != null) {
            ServicePackage servicePackage = servicePackageRepository.findById(request.getPackageId())
                    .orElseThrow(() -> new NotFoundException("Service package not found with id: " + request.getPackageId()));
            existingSubscription.setServicePackage(servicePackage);

            // Tính lại end date khi đổi gói
            LocalDate endDate = existingSubscription.getStartDate().plusDays(servicePackage.getDuration());
            existingSubscription.setEndDate(endDate);

            // ✅ Reset remainingSwaps khi đổi gói
            existingSubscription.setRemainingSwaps(servicePackage.getMaxSwaps());
        }

        return driverSubscriptionRepository.save(existingSubscription);
    }

    @Transactional
    public void deleteSubscription(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        DriverSubscription subscription = driverSubscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver subscription not found with id: " + id));

        driverSubscriptionRepository.delete(subscription);
    }
}