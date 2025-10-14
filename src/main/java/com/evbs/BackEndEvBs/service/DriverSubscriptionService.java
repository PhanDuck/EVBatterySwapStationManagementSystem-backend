package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.DriverSubscriptionRequest;
import com.evbs.BackEndEvBs.repository.DriverSubscriptionRepository;
import com.evbs.BackEndEvBs.repository.ServicePackageRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverSubscriptionService {

    @Autowired
    private final DriverSubscriptionRepository driverSubscriptionRepository;

    @Autowired
    private final ServicePackageRepository servicePackageRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    @Transactional
    public DriverSubscription createSubscription(DriverSubscriptionRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can create subscriptions");
        }

        ServicePackage servicePackage = servicePackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new NotFoundException("Service package not found with id: " + request.getPackageId()));

        // Tự động lấy ngày hiện tại
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(servicePackage.getDuration());

        DriverSubscription subscription = new DriverSubscription();
        subscription.setDriver(currentUser);
        subscription.setServicePackage(servicePackage);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(DriverSubscription.Status.ACTIVE);

        return driverSubscriptionRepository.save(subscription);
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