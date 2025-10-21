package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service quản lý sức khỏe pin và cảnh báo bảo trì
 * 
 * LOGIC:
 * - StateOfHealth (SOH) đo độ khỏe pin (0-100%)
 * - SOH giảm theo số lần sử dụng và thời gian
 * - SOH < 80%: Cần theo dõi
 * - SOH < 70%: Cần bảo trì gấp
 * - SOH < 60%: Tự động chuyển vào MAINTENANCE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatteryHealthService {

    private final BatteryRepository batteryRepository;

    //  Ngưỡng cảnh báo SOH
    private static final BigDecimal SOH_WARNING_THRESHOLD = BigDecimal.valueOf(80.0);      // Cảnh báo theo dõi
    private static final BigDecimal SOH_CRITICAL_THRESHOLD = BigDecimal.valueOf(70.0);     // Cảnh báo bảo trì gấp
    private static final BigDecimal SOH_MAINTENANCE_THRESHOLD = BigDecimal.valueOf(60.0);  // Tự động bảo trì
    
    //  Tốc độ giảm SOH
    private static final int USAGE_COUNT_PER_SOH_DROP = 50;  // Giảm 0.5% mỗi 50 lần sử dụng
    private static final BigDecimal SOH_DROP_RATE = BigDecimal.valueOf(0.5);  // Giảm 0.5% SOH

    /**
     * Scheduled job chạy mỗi ngày lúc 2:00 AM để kiểm tra sức khỏe pin
     * Cron: 0 0 2 * * * = 2:00 AM mỗi ngày
     */
    @Scheduled(cron = "0 0 2 * * *")  // 2:00 AM mỗi ngày
    @Transactional
    public void dailyBatteryHealthCheck() {
        log.info(" [Battery Health] Starting daily health check...");

        List<Battery> allBatteries = batteryRepository.findAll();
        
        if (allBatteries.isEmpty()) {
            log.info(" [Battery Health] No batteries found.");
            return;
        }

        int checkedCount = 0;
        int warningCount = 0;
        int criticalCount = 0;
        int maintenanceCount = 0;

        List<Battery> batteriesNeedingMaintenance = new ArrayList<>();

        for (Battery battery : allBatteries) {
            try {
                HealthStatus status = checkBatteryHealth(battery);
                checkedCount++;

                switch (status) {
                    case WARNING:
                        warningCount++;
                        break;
                    case CRITICAL:
                        criticalCount++;
                        batteriesNeedingMaintenance.add(battery);
                        break;
                    case MAINTENANCE_REQUIRED:
                        maintenanceCount++;
                        moveBatteryToMaintenance(battery);
                        batteriesNeedingMaintenance.add(battery);
                        break;
                    default:
                        // HEALTHY - không làm gì
                        break;
                }
            } catch (Exception e) {
                log.error(" [Battery Health] Error checking battery {}: {}", battery.getId(), e.getMessage());
            }
        }


        // TODO: Gửi email/notification cho Admin về pin cần bảo trì
        if (!batteriesNeedingMaintenance.isEmpty()) {
            notifyAdminAboutMaintenanceNeeds(batteriesNeedingMaintenance);
        }
    }

    /**
     * Kiểm tra sức khỏe của 1 pin
     */
    private HealthStatus checkBatteryHealth(Battery battery) {
        BigDecimal soh = battery.getStateOfHealth();
        
        if (soh == null) {
            log.warn("[Battery {}] SOH is null, setting to 100%", battery.getId());
            battery.setStateOfHealth(BigDecimal.valueOf(100.0));
            batteryRepository.save(battery);
            return HealthStatus.HEALTHY;
        }

        // Kiểm tra ngưỡng
        if (soh.compareTo(SOH_MAINTENANCE_THRESHOLD) < 0) {
            log.error(" [Battery {}]  MAINTENANCE REQUIRED! SOH = {:.1f}%",
                     battery.getId(), soh.doubleValue());
            return HealthStatus.MAINTENANCE_REQUIRED;
        } else if (soh.compareTo(SOH_CRITICAL_THRESHOLD) < 0) {
            log.warn(" [Battery {}]  CRITICAL! SOH = {:.1f}% - Needs maintenance soon!",
                    battery.getId(), soh.doubleValue());
            return HealthStatus.CRITICAL;
        } else if (soh.compareTo(SOH_WARNING_THRESHOLD) < 0) {
            log.warn(" [Battery {}]  WARNING! SOH = {:.1f}% - Monitor closely",
                    battery.getId(), soh.doubleValue());
            return HealthStatus.WARNING;
        }

        return HealthStatus.HEALTHY;
    }

    /**
     * Tự động chuyển pin vào trạng thái bảo trì
     */
    @Transactional
    public void moveBatteryToMaintenance(Battery battery) {
        // Lưu trạm hiện tại để biết lấy pin từ đâu
        if (battery.getCurrentStation() != null) {
            log.info("[Battery {}] Moving to MAINTENANCE from Station {}",
                    battery.getId(), battery.getCurrentStation().getId());
        } else {
            log.info(" [Battery {}] Moving to MAINTENANCE (no current station)", battery.getId());
        }

        battery.setStatus(Battery.Status.MAINTENANCE);
        battery.setLastMaintenanceDate(LocalDate.now());
        batteryRepository.save(battery);

        log.info(" [Battery {}] Status changed to MAINTENANCE. SOH: {:.1f}%",
                battery.getId(), 
                battery.getStateOfHealth() != null ? battery.getStateOfHealth().doubleValue() : 0);
    }

    /**
     * Giảm SOH sau mỗi lần sử dụng (gọi từ SwapTransactionService)
     */
    @Transactional
    public void degradeSOHAfterUsage(Battery battery) {
        Integer usageCount = battery.getUsageCount();
        if (usageCount == null) {
            usageCount = 0;
        }

        // Giảm SOH mỗi USAGE_COUNT_PER_SOH_DROP lần sử dụng
        if (usageCount % USAGE_COUNT_PER_SOH_DROP == 0 && usageCount > 0) {
            BigDecimal currentSOH = battery.getStateOfHealth();
            if (currentSOH == null) {
                currentSOH = BigDecimal.valueOf(100.0);
            }

            BigDecimal newSOH = currentSOH.subtract(SOH_DROP_RATE);
            
            // Không cho SOH < 0
            if (newSOH.compareTo(BigDecimal.ZERO) < 0) {
                newSOH = BigDecimal.ZERO;
            }

            battery.setStateOfHealth(newSOH);
            batteryRepository.save(battery);

            log.info(" [Battery {}] SOH degraded: {:.1f}% → {:.1f}% (after {} uses)",
                    battery.getId(), 
                    currentSOH.doubleValue(), 
                    newSOH.doubleValue(), 
                    usageCount);

            // Kiểm tra ngay nếu SOH giảm xuống ngưỡng nguy hiểm
            if (newSOH.compareTo(SOH_MAINTENANCE_THRESHOLD) < 0) {
                log.error(" [Battery {}]  SOH dropped below 60%! Moving to MAINTENANCE!", battery.getId());
                moveBatteryToMaintenance(battery);
            } else if (newSOH.compareTo(SOH_CRITICAL_THRESHOLD) < 0) {
                log.warn(" [Battery {}]  SOH below 70%! Critical maintenance needed soon!", battery.getId());
            }
        }
    }

    /**
     * Thông báo Admin về pin cần bảo trì
     */
    private void notifyAdminAboutMaintenanceNeeds(List<Battery> batteries) {
        log.info(" [Notification] Sending maintenance alert to Admin...");
        log.info(" Batteries needing maintenance:");
        
        for (Battery battery : batteries) {
            log.info("   - Battery ID: {}, SOH: {:.1f}%, Station: {}", 
                    battery.getId(),
                    battery.getStateOfHealth() != null ? battery.getStateOfHealth().doubleValue() : 0,
                    battery.getCurrentStation() != null ? battery.getCurrentStation().getName() : "N/A");
        }

        // TODO: Implement actual notification system
        // - Email notification
        // - In-app notification
        // - Push notification
        log.info(" [Notification] Alert sent successfully!");
    }

    /**
     * Lấy danh sách pin cần bảo trì (cho Admin)
     */
    public List<Battery> getBatteriesNeedingMaintenance() {
        List<Battery> allBatteries = batteryRepository.findAll();
        List<Battery> needMaintenance = new ArrayList<>();

        for (Battery battery : allBatteries) {
            BigDecimal soh = battery.getStateOfHealth();
            if (soh != null && soh.compareTo(SOH_CRITICAL_THRESHOLD) < 0) {
                needMaintenance.add(battery);
            }
        }

        return needMaintenance;
    }

    /**
     * Lấy danh sách pin trong trạng thái MAINTENANCE
     */
    public List<Battery> getBatteriesInMaintenance() {
        return batteryRepository.findByStatus(Battery.Status.MAINTENANCE);
    }

    /**
     * Admin hoàn thành bảo trì pin
     * Logic tự động: SOH >= 70% → AVAILABLE, SOH < 70% → MAINTENANCE
     */
    @Transactional
    public void completeMaintenance(Battery battery, BigDecimal newSOH) {
        // Validation: Pin phải ở trong kho (currentStation = null)
        if (battery.getCurrentStation() != null) {
            throw new IllegalArgumentException(
                "Chỉ có thể bảo trì pin đang ở trong kho. Pin này đang ở trạm: " + battery.getCurrentStation().getName()
            );
        }
        
        BigDecimal oldSOH = battery.getStateOfHealth();
        battery.setStateOfHealth(newSOH);
        battery.setLastMaintenanceDate(LocalDate.now());
        battery.setUsageCount(0);  // Reset usage count sau bảo trì
        
        // Logic tự động: Kiểm tra SOH để quyết định status
        if (newSOH.compareTo(SOH_CRITICAL_THRESHOLD) >= 0) {
            // SOH >= 70% → Pin sẵn sàng sử dụng
            battery.setStatus(Battery.Status.AVAILABLE);
            log.info(" [Battery {}] Maintenance completed. SOH: {:.1f}% → {:.1f}%. Status: AVAILABLE",
                    battery.getId(),
                    oldSOH != null ? oldSOH.doubleValue() : 0,
                    newSOH.doubleValue());
        } else {
            // SOH < 70% → Pin vẫn cần bảo trì
            battery.setStatus(Battery.Status.MAINTENANCE);
            log.warn(" [Battery {}] SOH updated but still needs maintenance. SOH: {:.1f}% → {:.1f}%. Status: MAINTENANCE",
                    battery.getId(),
                    oldSOH != null ? oldSOH.doubleValue() : 0,
                    newSOH.doubleValue());
        }
        
        batteryRepository.save(battery);
    }

    /**
     * Enum trạng thái sức khỏe pin
     */
    private enum HealthStatus {
        HEALTHY,              
        WARNING,
        CRITICAL,
        MAINTENANCE_REQUIRED
    }
}
