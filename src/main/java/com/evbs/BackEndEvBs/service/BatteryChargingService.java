package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service tự động sạc pin
 * 
 * LOGIC:
 * - Pin status = CHARGING sẽ tự động tăng chargeLevel theo thời gian
 * - Thời gian sạc đầy: 3-4 giờ (configurable)
 * - Chạy mỗi 15 phút để update chargeLevel
 * - Khi chargeLevel >= 100% → Đổi status thành AVAILABLE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatteryChargingService {

    private final BatteryRepository batteryRepository;
    private final BatteryHistoryService batteryHistoryService;

    // ⚙️ Cấu hình thời gian sạc
    private static final long FULL_CHARGE_HOURS = 4;  // 4 giờ để sạc đầy từ 0% → 100%
    private static final BigDecimal CHARGE_RATE_PER_HOUR = BigDecimal.valueOf(100.0 / FULL_CHARGE_HOURS);  // 25% per hour

    /**
     * Scheduled job chạy mỗi 15 phút để update chargeLevel của pin đang sạc
     * Cron: 0 15 * * * * = Mỗi 15 phút
     */
    @Scheduled(cron = "0 */15 * * * *")  // Chạy mỗi 15 phút
    @Transactional
    public void autoChargeBatteries() {
        log.info("🔋 [Auto Charging] Starting battery charging update...");

        // Tìm tất cả pin đang CHARGING
        List<Battery> chargingBatteries = batteryRepository.findByStatus(Battery.Status.CHARGING);
        
        if (chargingBatteries.isEmpty()) {
            log.info("🔋 [Auto Charging] No batteries currently charging.");
            return;
        }

        log.info("🔋 [Auto Charging] Found {} batteries charging", chargingBatteries.size());

        int updatedCount = 0;
        int fullyChargedCount = 0;

        for (Battery battery : chargingBatteries) {
            try {
                boolean updated = updateBatteryCharge(battery);
                if (updated) {
                    updatedCount++;
                    if (battery.getStatus() == Battery.Status.AVAILABLE) {
                        fullyChargedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("🔋 [Auto Charging] Error updating battery {}: {}", battery.getId(), e.getMessage());
            }
        }

        log.info("🔋 [Auto Charging] Completed: {} batteries updated, {} fully charged", 
                 updatedCount, fullyChargedCount);
    }

    /**
     * Update chargeLevel của 1 pin dựa trên thời gian sạc
     * @return true nếu có update, false nếu không
     */
    private boolean updateBatteryCharge(Battery battery) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime chargeStartTime = battery.getLastChargedTime();

        // Nếu chưa set lastChargedTime → Set ngay bây giờ
        if (chargeStartTime == null) {
            battery.setLastChargedTime(now);
            batteryRepository.save(battery);
            log.info("🔋 [Battery {}] Started charging at {}", battery.getId(), now);
            return true;
        }

        // Tính thời gian đã sạc (giờ)
        long minutesCharged = ChronoUnit.MINUTES.between(chargeStartTime, now);
        double hoursCharged = minutesCharged / 60.0;

        // Tính chargeLevel hiện tại dựa trên thời gian
        BigDecimal currentCharge = battery.getChargeLevel() != null 
                ? battery.getChargeLevel() 
                : BigDecimal.ZERO;

        // Tính chargeLevel mới = chargeLevel cũ + (giờ đã sạc × tốc độ sạc)
        BigDecimal chargeIncrease = CHARGE_RATE_PER_HOUR.multiply(BigDecimal.valueOf(hoursCharged));
        BigDecimal newChargeLevel = currentCharge.add(chargeIncrease);

        // Cap ở 100%
        if (newChargeLevel.compareTo(BigDecimal.valueOf(100)) >= 0) {
            newChargeLevel = BigDecimal.valueOf(100.0);
            battery.setStatus(Battery.Status.AVAILABLE);  // ✅ Đã sạc đầy → AVAILABLE
            
            // 📝 GHI LỊCH SỬ: Pin sạc đầy
            batteryHistoryService.logBatteryEvent(battery, "CHARGED");
            
            log.info("🔋 [Battery {}] ✅ FULLY CHARGED! 100% (charged for {:.1f} hours)", 
                     battery.getId(), hoursCharged);
        } else {
            log.info("🔋 [Battery {}] Charging: {:.1f}% → {:.1f}% ({:.1f} hours)", 
                     battery.getId(), 
                     currentCharge.doubleValue(), 
                     newChargeLevel.doubleValue(), 
                     hoursCharged);
        }

        battery.setChargeLevel(newChargeLevel);
        batteryRepository.save(battery);
        
        return true;
    }

    /**
     * Bắt đầu sạc pin (gọi khi pin được đưa vào trạm)
     * @param battery Pin cần sạc
     * @param initialChargeLevel Mức pin ban đầu (0-100%)
     */
    @Transactional
    public void startCharging(Battery battery, BigDecimal initialChargeLevel) {
        battery.setStatus(Battery.Status.CHARGING);
        battery.setChargeLevel(initialChargeLevel);
        battery.setLastChargedTime(LocalDateTime.now());
        batteryRepository.save(battery);
        
        // 📝 GHI LỊCH SỬ: Pin bắt đầu sạc
        batteryHistoryService.logBatteryEvent(battery, "CHARGING");
        
        log.info("🔋 [Battery {}] Started charging from {:.1f}%", 
                 battery.getId(), initialChargeLevel.doubleValue());
    }

    /**
     * Dừng sạc pin (manual stop)
     */
    @Transactional
    public void stopCharging(Battery battery) {
        battery.setStatus(Battery.Status.AVAILABLE);
        battery.setLastChargedTime(null);
        batteryRepository.save(battery);
        
        log.info("🔋 [Battery {}] Charging stopped. Current level: {:.1f}%", 
                 battery.getId(), 
                 battery.getChargeLevel() != null ? battery.getChargeLevel().doubleValue() : 0);
    }

    /**
     * Tính thời gian còn lại để sạc đầy (phút)
     */
    public long getEstimatedTimeToFull(Battery battery) {
        if (battery.getStatus() != Battery.Status.CHARGING) {
            return 0;
        }

        BigDecimal currentCharge = battery.getChargeLevel() != null 
                ? battery.getChargeLevel() 
                : BigDecimal.ZERO;

        BigDecimal remainingCharge = BigDecimal.valueOf(100).subtract(currentCharge);
        double hoursRemaining = remainingCharge.doubleValue() / CHARGE_RATE_PER_HOUR.doubleValue();
        
        return Math.round(hoursRemaining * 60);  // Convert to minutes
    }
}
