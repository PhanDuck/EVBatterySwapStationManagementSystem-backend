package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.service.BatteryHealthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller để Admin quản lý sức khỏe và bảo trì pin
 */
@RestController
@RequestMapping("/api/battery-health")
@RequiredArgsConstructor
@SecurityRequirement(name = "api")
public class BatteryHealthController {

    private final BatteryHealthService batteryHealthService;
    private final BatteryRepository batteryRepository;

    /**
     * Lấy danh sách tất cả pin cần bảo trì (SOH < 70%)
     * Chỉ Admin và Staff mới xem được
     */
    @GetMapping("/needs-maintenance")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getBatteriesNeedingMaintenance() {
        List<Battery> batteries = batteryHealthService.getBatteriesNeedingMaintenance();
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", batteries.size());
        response.put("batteries", batteries);
        response.put("message", batteries.isEmpty() 
            ? "Không có pin nào cần bảo trì" 
            : batteries.size() + " pin cần bảo trì");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách pin đang trong trạng thái MAINTENANCE
     */
    @GetMapping("/in-maintenance")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getBatteriesInMaintenance() {
        List<Battery> batteries = batteryHealthService.getBatteriesInMaintenance();
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", batteries.size());
        response.put("batteries", batteries);
        response.put("message", batteries.isEmpty() 
            ? "Không có pin nào đang bảo trì" 
            : batteries.size() + " pin đang bảo trì");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Admin chuyển pin vào trạng thái bảo trì thủ công
     * POST /api/battery-health/{batteryId}/move-to-maintenance
     */
    @PostMapping("/{batteryId}/move-to-maintenance")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> moveBatteryToMaintenance(@PathVariable Long batteryId) {
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found with ID: " + batteryId));
        
        batteryHealthService.moveBatteryToMaintenance(battery);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Pin đã được chuyển vào trạng thái bảo trì");
        response.put("battery", battery);
        response.put("batteryId", batteryId);
        response.put("status", battery.getStatus());
        response.put("stateOfHealth", battery.getStateOfHealth());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Admin hoàn thành bảo trì pin
     * PUT /api/battery-health/{batteryId}/complete-maintenance
     * 
     * Request body: { "newSOH": 95.0 }
     */
    @PutMapping("/{batteryId}/complete-maintenance")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> completeMaintenance(
            @PathVariable Long batteryId,
            @RequestBody Map<String, Double> request) {
        
        Double newSOHValue = request.get("newSOH");
        if (newSOHValue == null || newSOHValue < 0 || newSOHValue > 100) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "SOH phải từ 0-100%"
            ));
        }

        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Battery not found with ID: " + batteryId));
        
        BigDecimal newSOH = BigDecimal.valueOf(newSOHValue);
        batteryHealthService.completeMaintenance(battery, newSOH);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bảo trì hoàn tất. Pin đã sẵn sàng sử dụng.");
        response.put("battery", battery);
        response.put("batteryId", batteryId);
        response.put("newSOH", newSOHValue);
        response.put("status", battery.getStatus());
        response.put("usageCount", battery.getUsageCount());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Chạy health check thủ công (không cần đợi scheduled job)
     */
    @PostMapping("/run-health-check")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> runHealthCheckManually() {
        batteryHealthService.dailyBatteryHealthCheck();
        
        return ResponseEntity.ok(Map.of(
            "message", "Health check đã chạy thành công",
            "timestamp", java.time.LocalDateTime.now()
        ));
    }
}
