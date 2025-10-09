package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.BatteryRequest;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatteryService {

    @Autowired
    private final BatteryRepository batteryRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    /**
     * CREATE - Tạo battery mới (Admin/Staff only)
     */
    @Transactional
    public Battery createBattery(BatteryRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Battery battery = modelMapper.map(request, Battery.class);
        return batteryRepository.save(battery);
    }

    /**
     * READ - Lấy tất cả batteries (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Battery> getAllBatteries() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryRepository.findAll();
    }

    /**
     * READ - Lấy battery theo ID (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public Battery getBatteryById(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found"));
    }

    /**
     * READ - Lấy available batteries (Public)
     */
    @Transactional(readOnly = true)
    public List<Battery> getAvailableBatteries() {
        return batteryRepository.findByStatus("Available");
    }

    /**
     * UPDATE - Cập nhật battery (Admin/Staff only)
     */
    @Transactional
    public Battery updateBattery(Long id, BatteryRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        modelMapper.map(request, battery);
        return batteryRepository.save(battery);
    }

    /**
     * DELETE - Xóa battery (Admin only)
     */
    @Transactional
    public void deleteBattery(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found"));
        batteryRepository.delete(battery);
    }

    /**
     * SEARCH - Tìm batteries theo model (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Battery> searchBatteriesByModel(String model) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return batteryRepository.findByModelContainingIgnoreCase(model);
    }

    /**
     * UPDATE - Cập nhật battery status (Admin/Staff only)
     */
    @Transactional
    public Battery updateBatteryStatus(Long id, String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        battery.setStatus(status);
        return batteryRepository.save(battery);
    }

    /**
     * UPDATE - Cập nhật battery health (Admin/Staff only)
     */
    @Transactional
    public Battery updateBatteryHealth(Long id, BigDecimal stateOfHealth) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        battery.setStateOfHealth(stateOfHealth);
        return batteryRepository.save(battery);
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}