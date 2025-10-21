package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.StationInventory;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.StationInventoryRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StationInventoryService {

    private final StationInventoryRepository stationInventoryRepository;
    private final BatteryRepository batteryRepository;
    private final StationRepository stationRepository;

    public StationInventoryService(StationInventoryRepository stationInventoryRepository,
                                  BatteryRepository batteryRepository,
                                  StationRepository stationRepository) {
        this.stationInventoryRepository = stationInventoryRepository;
        this.batteryRepository = batteryRepository;
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public List<StationInventory> getAllBatteriesInWarehouse() {
        return stationInventoryRepository.findAll();
    }

    /**
     * Thay pin bảo trì ở trạm bằng pin từ kho tổng
     * 
     * Điều kiện:
     * - Pin cũ: Ở trạm, status = MAINTENANCE
     * - Pin mới: Trong kho (currentStation = NULL), CÙNG BatteryType, health >= 90%
     * 
     * Kết quả:
     * - Pin mới → Vào trạm (currentStation = stationId)
     * - Pin cũ → Về kho (currentStation = NULL, thêm vào StationInventory)
     */
    @Transactional
    public Map<String, Object> replaceBatteryForMaintenance(
            Long maintenanceBatteryId, 
            Long availableBatteryId, 
            Long stationId) {
        
        // 1. Kiểm tra trạm
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay tram voi ID: " + stationId));
        
        // 2. Kiểm tra pin cũ (MAINTENANCE ở trạm)
        Battery maintenanceBattery = batteryRepository.findById(maintenanceBatteryId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay pin voi ID: " + maintenanceBatteryId));
        
        // 3. Kiểm tra pin mới (AVAILABLE trong kho)
        Battery availableBattery = batteryRepository.findById(availableBatteryId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay pin voi ID: " + availableBatteryId));
        
        // 4. Validate pin cũ: Phải ở TRẠM này
        if (maintenanceBattery.getCurrentStation() == null || 
            !maintenanceBattery.getCurrentStation().getId().equals(stationId)) {
            throw new RuntimeException("Pin cu khong o tram nay");
        }
        
        // 5. Validate pin cũ: Phải MAINTENANCE
        if (maintenanceBattery.getStatus() != Battery.Status.MAINTENANCE) {
            throw new RuntimeException("Pin cu khong o trang thai MAINTENANCE");
        }
        
        // 6. Validate pin mới: Phải TRONG KHO (currentStation = NULL)
        if (availableBattery.getCurrentStation() != null) {
            throw new RuntimeException("Pin moi da o tram " + availableBattery.getCurrentStation().getName());
        }
        
        // 7. Validate pin mới: Sức khỏe >= 90%
        if (availableBattery.getStateOfHealth() == null || 
            availableBattery.getStateOfHealth().compareTo(new BigDecimal("90.00")) < 0) {
            throw new RuntimeException("Pin moi suc khoe khong du (can >= 90%)");
        }
        
        // 8. Validate: CÙNG LOẠI PIN (BatteryType phải giống nhau)
        if (!maintenanceBattery.getBatteryType().getId().equals(availableBattery.getBatteryType().getId())) {
            throw new RuntimeException(
                String.format("Khong duoc thay pin khac loai! Pin cu: %s, Pin moi: %s", 
                    maintenanceBattery.getBatteryType().getName(),
                    availableBattery.getBatteryType().getName())
            );
        }
        
        // 9. Đưa pin mới VÀO TRẠM
        availableBattery.setCurrentStation(station);
        availableBattery.setStatus(Battery.Status.AVAILABLE);
        
        // 10. Đưa pin cũ VỀ KHO
        maintenanceBattery.setCurrentStation(null);
        maintenanceBattery.setStatus(Battery.Status.MAINTENANCE);
        maintenanceBattery.setLastMaintenanceDate(LocalDate.now());
        
        // 11. Lưu cả hai pin
        batteryRepository.save(maintenanceBattery);
        batteryRepository.save(availableBattery);
        
        // 12. Tạo StationInventory cho pin cũ về kho
        StationInventory inventory = new StationInventory();
        inventory.setBattery(maintenanceBattery);
        inventory.setStatus(StationInventory.Status.MAINTENANCE);
        inventory.setLastUpdate(LocalDateTime.now());
        stationInventoryRepository.save(inventory);
        
        // 13. Xóa pin mới khỏi StationInventory (nếu có)
        stationInventoryRepository.findAll().stream()
                .filter(inv -> inv.getBattery().getId().equals(availableBatteryId))
                .findFirst()
                .ifPresent(stationInventoryRepository::delete);
        
        log.info(" Da thay pin tai tram {}. Pin cu {} VE KHO, Pin moi {} VAO TRAM",
                station.getName(), maintenanceBattery.getId(), availableBattery.getId());
        
        // 14. Trả về kết quả
        Map<String, Object> result = new HashMap<>();
        result.put("stationId", station.getId());
        result.put("stationName", station.getName());
        result.put("oldBattery", Map.of(
            "batteryId", maintenanceBattery.getId(),
            "model", maintenanceBattery.getModel(),
            "batteryType", maintenanceBattery.getBatteryType().getName(),
            "oldLocation", "TRAM " + station.getName(),
            "newLocation", "KHO TONG",
            "status", "MAINTENANCE",
            "stateOfHealth", maintenanceBattery.getStateOfHealth()
        ));
        result.put("newBattery", Map.of(
            "batteryId", availableBattery.getId(),
            "model", availableBattery.getModel(),
            "batteryType", availableBattery.getBatteryType().getName(),
            "oldLocation", "KHO TONG",
            "newLocation", "TRAM " + station.getName(),
            "status", "AVAILABLE",
            "stateOfHealth", availableBattery.getStateOfHealth()
        ));
        result.put("replacedAt", LocalDateTime.now());
        
        return result;
    }
}
