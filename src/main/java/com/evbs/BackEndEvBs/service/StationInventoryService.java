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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Kiểm tra capacity của station
        validateStationCapacity(station);

        StationInventory stationInventory = new StationInventory();
        stationInventory.setStation(station);
        stationInventory.setBattery(battery);
        stationInventory.setStatus(request.getStatus());
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
        return stationInventoryRepository.findByStationIdAndStatus(stationId, StationInventory.Status.AVAILABLE);
    }

    /**
     * READ - Lấy thông tin capacity của station (Public)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStationCapacityInfo(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found"));
        
        List<StationInventory> currentInventory = stationInventoryRepository.findByStationId(stationId);
        int currentCount = currentInventory.size();
        int maxCapacity = station.getCapacity();
        int availableSlots = maxCapacity - currentCount;
        
        Map<String, Object> capacityInfo = new HashMap<>();
        capacityInfo.put("stationId", stationId);
        capacityInfo.put("stationName", station.getName());
        capacityInfo.put("maxCapacity", maxCapacity);
        capacityInfo.put("currentCount", currentCount);
        capacityInfo.put("availableSlots", availableSlots);
        capacityInfo.put("isFull", currentCount >= maxCapacity);
        
        return capacityInfo;
    }

    /**
     * UPDATE - Cập nhật battery status (Admin/Staff only)
     */
    @Transactional
    public StationInventory updateBatteryStatus(Long id, StationInventory.Status status) {
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

    /**
     * Kiểm tra station đã đầy chưa
     */
    private void validateStationCapacity(Station station) {
        int currentBatteryCount = stationInventoryRepository.findByStationId(station.getId()).size();
        if (currentBatteryCount >= station.getCapacity()) {
            throw new AuthenticationException(
                String.format("Station '%s' is full! Current: %d/%d batteries. Cannot add more batteries.", 
                    station.getName(), currentBatteryCount, station.getCapacity())
            );
        }
    }
}