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

            // Reset remainingSwaps khi đổi gói
            existingSubscription.setRemainingSwaps(servicePackage.getMaxSwaps());
        }

        return driverSubscriptionRepository.save(existingSubscription);
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
}