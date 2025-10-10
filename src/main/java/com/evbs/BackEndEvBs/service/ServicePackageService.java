package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.ServicePackageRequest;
import com.evbs.BackEndEvBs.model.request.ServicePackageUpdateRequest;
import com.evbs.BackEndEvBs.repository.ServicePackageRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicePackageService {

    @Autowired
    private final ServicePackageRepository servicePackageRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    @Transactional
    public ServicePackage createServicePackage(ServicePackageRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        if (servicePackageRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Service package with name '" + request.getName() + "' already exists");
        }

        ServicePackage servicePackage = modelMapper.map(request, ServicePackage.class);
        return servicePackageRepository.save(servicePackage);
    }

    @Transactional(readOnly = true)
    public List<ServicePackage> getAllServicePackages() {
        return servicePackageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ServicePackage getServicePackageById(Long id) {
        return servicePackageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service package not found with id: " + id));
    }

    @Transactional
    public ServicePackage updateServicePackage(Long id, ServicePackageUpdateRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        ServicePackage existingPackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service package not found with id: " + id));

        // Chỉ update name nếu có giá trị mới và không trùng
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            if (!existingPackage.getName().equals(request.getName()) &&
                    servicePackageRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Service package with name '" + request.getName() + "' already exists");
            }
            existingPackage.setName(request.getName());
        }

        // Chỉ update description nếu có giá trị mới
        if (request.getDescription() != null) {
            existingPackage.setDescription(request.getDescription());
        }

        // Chỉ update price nếu có giá trị mới
        if (request.getPrice() != null) {
            existingPackage.setPrice(request.getPrice());
        }

        // Chỉ update duration nếu có giá trị mới
        if (request.getDuration() != null) {
            existingPackage.setDuration(request.getDuration());
        }

        // Chỉ update maxSwaps nếu có giá trị mới
        if (request.getMaxSwaps() != null) {
            existingPackage.setMaxSwaps(request.getMaxSwaps());
        }

        return servicePackageRepository.save(existingPackage);
    }

    @Transactional
    public void deleteServicePackage(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        ServicePackage servicePackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service package not found with id: " + id));

        if (!servicePackage.getSubscriptions().isEmpty()) {
            throw new IllegalStateException("Cannot delete service package with active subscriptions");
        }

        servicePackageRepository.delete(servicePackage);
    }
}