package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.ServicePackage;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.ServicePackageRequest;
import com.evbs.BackEndEvBs.model.request.ServicePackageUpdateRequest;
import com.evbs.BackEndEvBs.repository.ServicePackageRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public ServicePackage createServicePackage(ServicePackageRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Truy cập bị từ chối. Chỉ quản trị viên (Admin) mới được phép thực hiện thao tác này.");
        }

        long currentPackageCount = servicePackageRepository.count();
        if (currentPackageCount >= 12) {
            throw new IllegalArgumentException("Maximum limit of 12 service packages reached. Cannot create more.");
        }

        if (servicePackageRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Gói dịch vụ với tên '" + request.getName() + "' đã tồn tại.");
        }

        //  Tạo service package thủ công thay vì dùng ModelMapper (tránh xung đột)
        ServicePackage servicePackage = new ServicePackage();
        servicePackage.setName(request.getName());
        servicePackage.setDescription(request.getDescription());
        servicePackage.setPrice(request.getPrice());
        servicePackage.setDuration(request.getDuration());
        servicePackage.setMaxSwaps(request.getMaxSwaps());

        return servicePackageRepository.save(servicePackage);
    }

    @Transactional(readOnly = true)
    public List<ServicePackage> getAllServicePackages() {
        return servicePackageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ServicePackage getServicePackageById(Long id) {
        return servicePackageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + id));
    }

    @Transactional
    public ServicePackage updateServicePackage(Long id, ServicePackageUpdateRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Truy cập bị từ chối. Chỉ quản trị viên (Admin) mới được phép thực hiện thao tác này.");
        }

        ServicePackage existingPackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + id));

        // Chỉ cập nhật tên nếu có giá trị mới và không bị trùng
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            if (!existingPackage.getName().equals(request.getName()) &&
                    servicePackageRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Gói dịch vụ với tên '" + request.getName() + "' đã tồn tại.");
            }
            existingPackage.setName(request.getName());
        }

        // Chỉ cập nhật mô tả nếu có giá trị mới
        if (request.getDescription() != null) {
            existingPackage.setDescription(request.getDescription());
        }

        // Chỉ cập nhật giá nếu có giá trị mới
        if (request.getPrice() != null) {
            existingPackage.setPrice(request.getPrice());
        }

        // Chỉ cập nhật thời hạn nếu có giá trị mới
        if (request.getDuration() != null) {
            existingPackage.setDuration(request.getDuration());
        }

        // Chỉ cập nhật số lần đổi tối đa nếu có giá trị mới
        if (request.getMaxSwaps() != null) {
            existingPackage.setMaxSwaps(request.getMaxSwaps());
        }

        return servicePackageRepository.save(existingPackage);
    }

    @Transactional
    public void deleteServicePackage(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Truy cập bị từ chối. Chỉ quản trị viên (Admin) mới được phép thực hiện thao tác này.");
        }

        ServicePackage servicePackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói dịch vụ với ID: " + id));

        if (!servicePackage.getSubscriptions().isEmpty()) {
            throw new IllegalStateException("Không thể xóa gói dịch vụ đang có người đăng ký hoạt động.");
        }

        servicePackageRepository.delete(servicePackage);
    }
}
