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
     * DEPRECATED - No longer used
     * Use createSubscriptionAfterPayment() after successful payment
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
                    "You already have an ACTIVE subscription! " +
                    "Current package: " + existing.getServicePackage().getName() + " " +
                    "(remaining " + existing.getRemainingSwaps() + " swaps). " +
                    "Please wait for expiration or cancel old package before buying new one."
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
     * TẠO SUBSCRIPTION SAU KHI THANH TOÁN THÀNH CÔNG
     * 
     * Được gọi từ MoMoService sau khi verify payment thành công
     * 
     * BUSINESS RULES:
     * - Có gói ACTIVE + còn lượt swap: KHÔNG cho mua gói khác
     * - Có gói ACTIVE + hết lượt swap (remainingSwaps = 0): CHO PHÉP mua gói khác
     * - Có gói EXPIRED: CHO PHÉP mua gói mới
     * - Không có gói: CHO PHÉP mua gói
     * 
     * WORKFLOW:
     * BUOC 1: Driver chọn packageId - Tạo MoMo payment URL
     * BUOC 2: Driver thanh toán trên MoMo
     * BUOC 3: MoMo callback - Gọi method này
     * BUOC 4: Kiểm tra gói hiện tại của driver
     * BUOC 5: Nếu có gói active + còn lượt - REJECT
     * BUOC 6: Nếu hết lượt hoặc hết hạn - EXPIRE gói cũ + TẠO gói mới
     * 
     * @param packageId ID của service package
     * @return DriverSubscription mới được tạo với status ACTIVE
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
            
            // Still has swaps remaining, not allowed to buy new package
            if (existingSub.getRemainingSwaps() > 0) {
                throw new AuthenticationException(
                    "You already have an ACTIVE subscription with remaining swaps! " +
                    "Current package: " + existingSub.getServicePackage().getName() + " " +
                    "(remaining " + existingSub.getRemainingSwaps() + " swaps, " +
                    "expires: " + existingSub.getEndDate() + "). " +
                    "Please use all remaining swaps before buying new package."
                );
            }
            
            // No swaps remaining (remainingSwaps = 0), allow new package purchase
            log.info("Driver {} has active subscription but 0 remaining swaps. Expiring old subscription...", 
                     currentUser.getEmail());
            
            // Expire gói cũ
            existingSub.setStatus(DriverSubscription.Status.EXPIRED);
            driverSubscriptionRepository.save(existingSub);
            
            log.info("Old subscription {} expired (was active but had 0 swaps)", existingSub.getId());
        }

        // Create new subscription (no active package or old package expired)
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(servicePackage.getDuration());

        DriverSubscription subscription = new DriverSubscription();
        subscription.setDriver(currentUser);
        subscription.setServicePackage(servicePackage);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(DriverSubscription.Status.ACTIVE); // Active immediately since already paid
        subscription.setRemainingSwaps(servicePackage.getMaxSwaps());

        DriverSubscription savedSubscription = driverSubscriptionRepository.save(subscription);
        
        log.info("Subscription created after payment: Driver {} -> Package {} ({} swaps, {} VND)", 
                 currentUser.getEmail(), 
                 servicePackage.getName(),
                 servicePackage.getMaxSwaps(),
                 servicePackage.getPrice());

        return savedSubscription;
    }

    /**
     * TẠO SUBSCRIPTION SAU KHI THANH TOÁN (Overload - Không cần token)
     * 
     * Được gọi từ MoMo callback khi driver KHÔNG CÓ TOKEN
     * 
     * TẠI SAO CẦN METHOD NÀY?
     * - Khi driver thanh toán - redirect ra MoMo app/website
     * - Sau khi thanh toán - MoMo callback về server
     * - Lúc này driver KHÔNG CÓ JWT TOKEN!
     * - Giải pháp: Lưu driverId vào extraData, lấy ra khi callback
     * 
     * LOGIC GIỐNG METHOD TRÊN:
     * - Validate package tồn tại
     * - Kiểm tra gói hiện tại
     * - Expire gói cũ nếu cần
     * - Tạo gói mới ACTIVE
     * 
     * @param packageId ID của service package
     * @param driverId ID của driver (lấy từ extraData trong MoMo callback)
     * @return DriverSubscription mới được tạo với status ACTIVE
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

    @Transactional
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
}