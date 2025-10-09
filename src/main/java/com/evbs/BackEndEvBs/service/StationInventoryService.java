package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.StationInventoryRequest;
import com.evbs.BackEndEvBs.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StationInventoryService {

    @Autowired
    private final StationInventoryRepository stationInventoryRepository;

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final BatteryRepository batteryRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    /**
     * CREATE - Thêm battery vào station (Admin/Staff only)
     */
    @Transactional
    public StationInventory addBatteryToStation(StationInventoryRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        // Validate station
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Station not found"));

        // Validate battery
        Battery battery = batteryRepository.findById(request.getBatteryId())
                .orElseThrow(() -> new NotFoundException("Battery not found"));

        // Kiểm tra battery đã có trong station khác chưa
        if (stationInventoryRepository.existsByBatteryId(request.getBatteryId())) {
            throw new AuthenticationException("Battery already exists in another station");
        }

        StationInventory stationInventory = modelMapper.map(request, StationInventory.class);
        stationInventory.setStation(station);
        stationInventory.setBattery(battery);
        stationInventory.setLastUpdate(LocalDateTime.now());

        // Cập nhật current station của battery
        battery.setCurrentStation(station);
        batteryRepository.save(battery);

        return stationInventoryRepository.save(stationInventory);
    }

    /**
     * READ - Lấy tất cả inventory (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<StationInventory> getAllInventory() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return stationInventoryRepository.findAll();
    }

    /**
     * READ - Lấy inventory theo station (Public)
     */
    @Transactional(readOnly = true)
    public List<StationInventory> getStationInventory(Long stationId) {
        return stationInventoryRepository.findByStationId(stationId);
    }

    /**
     * READ - Lấy available batteries trong station (Public)
     */
    @Transactional(readOnly = true)
    public List<StationInventory> getAvailableBatteries(Long stationId) {
        return stationInventoryRepository.findByStationIdAndStatus(stationId, "Available");
    }

    /**
     * UPDATE - Cập nhật battery status (Admin/Staff only)
     */
    @Transactional
    public StationInventory updateBatteryStatus(Long id, String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        StationInventory inventory = stationInventoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory not found"));

        inventory.setStatus(status);
        inventory.setLastUpdate(LocalDateTime.now());

        return stationInventoryRepository.save(inventory);
    }

    /**
     * DELETE - Xóa battery khỏi station (Admin only)
     */
    @Transactional
    public void removeBatteryFromStation(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied");
        }

        StationInventory inventory = stationInventoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory not found"));

        // Cập nhật current station của battery thành null
        Battery battery = inventory.getBattery();
        battery.setCurrentStation(null);
        batteryRepository.save(battery);

        stationInventoryRepository.delete(inventory);
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}