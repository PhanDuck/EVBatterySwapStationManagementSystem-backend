package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.BatteryRequest;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
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
    private final StationRepository stationRepository;

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
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Battery battery = modelMapper.map(request, Battery.class);

        // Set current station nếu có
        if (request.getCurrentStationId() != null) {
            Station station = stationRepository.findById(request.getCurrentStationId())
                    .orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getCurrentStationId()));
            battery.setCurrentStation(station);
        }

        return batteryRepository.save(battery);
    }

    /**
     * READ - Lấy tất cả batteries (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Battery> getAllBatteries() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
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
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        return batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found with id: " + id));
    }

    /**
     * READ - Lấy batteries theo station (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Battery> getBatteriesByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        // Verify station exists
        if (!stationRepository.existsById(stationId)) {
            throw new NotFoundException("Station not found with id: " + stationId);
        }

        return batteryRepository.findByCurrentStationId(stationId);
    }

    /**
     * READ - Lấy available batteries tại station (Public - Driver có thể xem)
     */
    @Transactional(readOnly = true)
    public List<Battery> getAvailableBatteriesAtStation(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + stationId));

        return batteryRepository.findByCurrentStationAndStatus(station, "Available");
    }

    /**
     * READ - Lấy batteries theo status (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Battery> getBatteriesByStatus(String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return batteryRepository.findByStatus(status);
    }

    /**
     * UPDATE - Cập nhật battery (Admin/Staff only)
     */
    @Transactional
    public Battery updateBattery(Long id, BatteryRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Battery existingBattery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found with id: " + id));

        // Sử dụng ModelMapper để cập nhật các field
        modelMapper.map(request, existingBattery);

        // Cập nhật station nếu có
        if (request.getCurrentStationId() != null) {
            Station station = stationRepository.findById(request.getCurrentStationId())
                    .orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getCurrentStationId()));
            existingBattery.setCurrentStation(station);
        } else if (request.getCurrentStationId() == null && existingBattery.getCurrentStation() != null) {
            // Remove from station if stationId is explicitly set to null
            existingBattery.setCurrentStation(null);
        }

        return batteryRepository.save(existingBattery);
    }

    /**
     * UPDATE - Chỉ cập nhật status (Staff có thể update status khi swap/charge)
     */
    @Transactional
    public Battery updateBatteryStatus(Long id, String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found with id: " + id));

        battery.setStatus(status);
        return batteryRepository.save(battery);
    }

    /**
     * UPDATE - Chỉ cập nhật state of health (System/Admin)
     */
    @Transactional
    public Battery updateBatterySOH(Long id, BigDecimal stateOfHealth) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        if (stateOfHealth.compareTo(BigDecimal.ZERO) < 0 || stateOfHealth.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("State of health must be between 0 and 100");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found with id: " + id));

        battery.setStateOfHealth(stateOfHealth);
        return batteryRepository.save(battery);
    }

    /**
     * DELETE - Xóa battery (Admin only)
     */
    @Transactional
    public void deleteBattery(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Battery not found with id: " + id));

        batteryRepository.delete(battery);
    }

    /**
     * Utility methods - Đếm số available batteries tại station
     */
    @Transactional(readOnly = true)
    public long countAvailableBatteriesAtStation(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + stationId));
        return batteryRepository.countByCurrentStationAndStatus(station, "Available");
    }

    /**
     * READ - Lấy batteries với SOH tốt (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Battery> getBatteriesWithGoodHealth(BigDecimal minSoh) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return batteryRepository.findByStateOfHealthGreaterThanEqual(minSoh);
    }

    /**
     * Helper method kiểm tra role
     */
    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}