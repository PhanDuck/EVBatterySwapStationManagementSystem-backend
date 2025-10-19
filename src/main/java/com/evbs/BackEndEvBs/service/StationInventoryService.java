package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.StationInventory;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.StationInventoryRequest;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.StationInventoryRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StationInventoryService {

    private final StationInventoryRepository stationInventoryRepository;
    private final StationRepository stationRepository;
    private final BatteryRepository batteryRepository;

    public StationInventoryService(StationInventoryRepository stationInventoryRepository,
                                  StationRepository stationRepository,
                                  BatteryRepository batteryRepository) {
        this.stationInventoryRepository = stationInventoryRepository;
        this.stationRepository = stationRepository;
        this.batteryRepository = batteryRepository;
    }

    /**
     * Thêm pin vào trạm (Staff/Admin)
     */
    @Transactional
    public StationInventory addBatteryToStation(StationInventoryRequest request) {
        // 1. Kiểm tra station tồn tại
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("❌ Không tìm thấy trạm với ID: " + request.getStationId()));

        // 2. Kiểm tra battery tồn tại
        Battery battery = batteryRepository.findById(request.getBatteryId())
                .orElseThrow(() -> new NotFoundException("❌ Không tìm thấy pin với ID: " + request.getBatteryId()));

        // 3. Kiểm tra battery đã có trong inventory chưa
        if (stationInventoryRepository.existsByBattery_Id(battery.getId())) {
            throw new RuntimeException("❌ Pin này đã có trong inventory của trạm khác");
        }

        // 4. Kiểm tra battery type phù hợp với station
        if (!battery.getBatteryType().getId().equals(station.getBatteryType().getId())) {
            throw new RuntimeException(
                    String.format("❌ Pin loại %s không phù hợp với trạm (yêu cầu %s)",
                            battery.getBatteryType().getName(),
                            station.getBatteryType().getName())
            );
        }

        // 5. Kiểm tra capacity của station
        int currentCount = stationInventoryRepository.countByStation_Id(station.getId());
        if (currentCount >= station.getCapacity()) {
            throw new RuntimeException(
                    String.format("❌ Trạm đã đầy (%d/%d). Không thể thêm pin mới",
                            currentCount, station.getCapacity())
            );
        }

        // 6. Tạo inventory record
        StationInventory inventory = new StationInventory();
        inventory.setStation(station);
        inventory.setBattery(battery);
        inventory.setStatus(StationInventory.Status.AVAILABLE);
        inventory.setLastUpdate(LocalDateTime.now());

        // 7. Cập nhật battery
        battery.setCurrentStation(station);
        battery.setStatus(Battery.Status.AVAILABLE);
        batteryRepository.save(battery);

        // 8. Lưu inventory
        StationInventory saved = stationInventoryRepository.save(inventory);

        log.info("✅ Đã thêm pin {} vào trạm {} (inventory ID: {})",
                battery.getId(), station.getName(), saved.getId());

        return saved;
    }

    /**
     * Xóa pin khỏi trạm (Admin)
     */
    @Transactional
    public void removeBatteryFromStation(Long inventoryId) {
        // 1. Tìm inventory
        StationInventory inventory = stationInventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("❌ Không tìm thấy inventory với ID: " + inventoryId));

        // 2. Kiểm tra battery có đang được dùng không
        Battery battery = inventory.getBattery();
        if (battery.getStatus() == Battery.Status.IN_USE) {
            throw new RuntimeException("❌ Không thể xóa pin đang được sử dụng");
        }

        // 3. Cập nhật battery
        battery.setCurrentStation(null);
        battery.setStatus(Battery.Status.AVAILABLE);
        batteryRepository.save(battery);

        // 4. Xóa inventory
        stationInventoryRepository.delete(inventory);

        log.info("✅ Đã xóa pin {} khỏi trạm {} (inventory ID: {})",
                battery.getId(), inventory.getStation().getName(), inventoryId);
    }

    /**
     * Cập nhật status của pin trong inventory (Staff/Admin)
     */
    @Transactional
    public StationInventory updateBatteryStatus(Long inventoryId, StationInventory.Status status) {
        // 1. Tìm inventory
        StationInventory inventory = stationInventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("❌ Không tìm thấy inventory với ID: " + inventoryId));

        // 2. Cập nhật status
        inventory.setStatus(status);
        inventory.setLastUpdate(LocalDateTime.now());

        // 3. Cập nhật battery status tương ứng
        Battery battery = inventory.getBattery();
        switch (status) {
            case AVAILABLE:
                battery.setStatus(Battery.Status.AVAILABLE);
                break;
            case RESERVED:
                battery.setStatus(Battery.Status.IN_USE);
                break;
            case MAINTENANCE:
                battery.setStatus(Battery.Status.MAINTENANCE);
                break;
        }
        batteryRepository.save(battery);

        // 4. Lưu inventory
        StationInventory saved = stationInventoryRepository.save(inventory);

        log.info("✅ Đã cập nhật status inventory {} thành {}", inventoryId, status);

        return saved;
    }

    /**
     * Lấy tất cả inventory (Admin/Staff)
     */
    @Transactional(readOnly = true)
    public List<StationInventory> getAllInventory() {
        return stationInventoryRepository.findAll();
    }

    /**
     * Lấy inventory của 1 trạm (Public)
     */
    @Transactional(readOnly = true)
    public List<StationInventory> getStationInventory(Long stationId) {
        // Kiểm tra station tồn tại
        if (!stationRepository.existsById(stationId)) {
            throw new NotFoundException("❌ Không tìm thấy trạm với ID: " + stationId);
        }

        return stationInventoryRepository.findByStation_Id(stationId);
    }

    /**
     * Lấy danh sách pin AVAILABLE tại trạm (Public)
     */
    @Transactional(readOnly = true)
    public List<StationInventory> getAvailableBatteries(Long stationId) {
        // Kiểm tra station tồn tại
        if (!stationRepository.existsById(stationId)) {
            throw new NotFoundException("❌ Không tìm thấy trạm với ID: " + stationId);
        }

        return stationInventoryRepository.findByStation_IdAndStatus(
                stationId, StationInventory.Status.AVAILABLE
        );
    }

    /**
     * Lấy thông tin capacity của trạm (Public)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStationCapacityInfo(Long stationId) {
        // 1. Tìm station
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("❌ Không tìm thấy trạm với ID: " + stationId));

        // 2. Đếm số lượng pin
        int totalBatteries = stationInventoryRepository.countByStation_Id(stationId);
        int availableBatteries = stationInventoryRepository
                .findByStation_IdAndStatus(stationId, StationInventory.Status.AVAILABLE)
                .size();
        int reservedBatteries = stationInventoryRepository
                .findByStation_IdAndStatus(stationId, StationInventory.Status.RESERVED)
                .size();
        int maintenanceBatteries = stationInventoryRepository
                .findByStation_IdAndStatus(stationId, StationInventory.Status.MAINTENANCE)
                .size();

        // 3. Tạo response
        Map<String, Object> info = new HashMap<>();
        info.put("stationId", stationId);
        info.put("stationName", station.getName());
        info.put("capacity", station.getCapacity());
        info.put("totalBatteries", totalBatteries);
        info.put("availableBatteries", availableBatteries);
        info.put("reservedBatteries", reservedBatteries);
        info.put("maintenanceBatteries", maintenanceBatteries);
        info.put("freeSlots", station.getCapacity() - totalBatteries);
        info.put("utilizationRate", totalBatteries * 100.0 / station.getCapacity());
        info.put("batteryType", station.getBatteryType().getName());

        return info;
    }
}
