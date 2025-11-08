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
 * - Khi chargeLevel >= 95% → AVAILABLE (nhưng vẫn sạc tiếp đến 100%)
 * - Khi chargeLevel >= 100% → Dừng sạc
 * - QUAN TRỌNG: Kiểm tra StateOfHealth - nếu < 70% → MAINTENANCE (không cho dùng)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatteryChargingService {

    private final BatteryRepository batteryRepository;

    // cấu hình thời gian sạc
    private static final long FULL_CHARGE_HOURS = 4;  // 4 giờ để sạc đầy từ 0% → 100%
    private static final BigDecimal CHARGE_RATE_PER_HOUR = BigDecimal.valueOf(100.0 / FULL_CHARGE_HOURS);  // 25% per hour
    
    // Ngưỡng sức khỏe pin
    private static final BigDecimal MIN_HEALTH_FOR_USE = BigDecimal.valueOf(70.0);  // < 70% phải bảo trì

    /**
     * Scheduled job chạy mỗi 15 phút để update chargeLevel của pin đang sạc
     * Cron: 0 15 * * * * = Mỗi 15 phút
     */
    @Scheduled(cron = "0 */15 * * * *")  // Chạy mỗi 15 phút
    @Transactional
    public void autoChargeBatteries() {
        log.info("Tự động sạc pin - Bắt đầu quá trình...");

        // Tìm tất cả pin đang CHARGING
        List<Battery> chargingBatteries = batteryRepository.findByStatus(Battery.Status.CHARGING);
        
        if (chargingBatteries.isEmpty()) {
            log.info("Tự động sạc pin - Không có pin nào đang sạc. Kết thúc.");
            return;
        }

        log.info("Tự động sạc Đã tìm thấy {} pin đang sạc", chargingBatteries.size());

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
                log.error("Tự động sạc Lỗi khi cập nhật pin {}: {}", battery.getId(), e.getMessage());
            }
        }

        log.info("Đã hoàn tất sạc tự động: {} pin đã được cập nhật, {} đã được sạc đầy",
                 updatedCount, fullyChargedCount);
    }

    /**
     * Update chargeLevel của 1 pin dựa trên thời gian sạc
     */
    private boolean updateBatteryCharge(Battery battery) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime chargeStartTime = battery.getLastChargedTime();

        // Nếu chưa set lastChargedTime → Set ngay bây giờ
        if (chargeStartTime == null) {
            battery.setLastChargedTime(now);
            batteryRepository.save(battery);
            log.info("Pin {} Bắt đầu sạc lúc{}", battery.getId(), now);
            return true;
        }

        BigDecimal currentCharge = battery.getChargeLevel() != null 
                ? battery.getChargeLevel() 
                : BigDecimal.ZERO;

        // Nếu đã đạt 100% thì dừng cập nhật (đã sạc xong)
        if (currentCharge.compareTo(BigDecimal.valueOf(100)) >= 0) {
            // Đảm bảo status = AVAILABLE và chargeLevel = 100%
            if (battery.getStatus() != Battery.Status.AVAILABLE) {
                battery.setStatus(Battery.Status.AVAILABLE);
                battery.setChargeLevel(BigDecimal.valueOf(100.0));
                battery.setLastChargedTime(null);  // Clear charge start time
                batteryRepository.save(battery);
                
                log.info("Pin {} ĐÃ SẠC ĐẦY! Chuyển sang SẴN SÀNG ở mức 100%", battery.getId());
                return true;
            }
            return false;  // Đã sạc xong từ trước
        }

        // Tính thời gian đã sạc (phút)
        long minutesCharged = ChronoUnit.MINUTES.between(chargeStartTime, now);
        
        // Chỉ update nếu đã qua ít nhất 1 phút (tránh spam update)
        if (minutesCharged < 1) {
            return false;
        }

        // Tính charge tăng lên = (phút đã sạc / 60) × tốc độ sạc per hour
        double hoursCharged = minutesCharged / 60.0;
        BigDecimal chargeIncrease = CHARGE_RATE_PER_HOUR.multiply(BigDecimal.valueOf(hoursCharged));
        BigDecimal newChargeLevel = currentCharge.add(chargeIncrease);

        // Cap ở 100%
        if (newChargeLevel.compareTo(BigDecimal.valueOf(100)) >= 0) {
            newChargeLevel = BigDecimal.valueOf(100.0);
            
            //  KIỂM TRA SỨC KHỎE PIN trước khi chuyển AVAILABLE
            BigDecimal health = battery.getStateOfHealth();
            if (health != null && health.compareTo(MIN_HEALTH_FOR_USE) < 0) {
                // Sức khỏe < 70% → MAINTENANCE (không cho dùng)
                battery.setStatus(Battery.Status.MAINTENANCE);
                battery.setLastChargedTime(null);
                
                log.warn("Pin {} \uFE0F Sạc 100% nhưng sức khỏe {:.1f}% < 70% → BẢO TRÌ",
                         battery.getId(), health.doubleValue());
            } else {
                // Sức khỏe tốt → AVAILABLE
                battery.setStatus(Battery.Status.AVAILABLE);
                battery.setLastChargedTime(null);  //  Dừng sạc khi đạt 100%
                
                log.info("Pin {} ĐÃ SẠC ĐẦY 100% → CÓ SẴN (tình trạng: {:.1f}%)",
                         battery.getId(), health != null ? health.doubleValue() : 100.0);
            }
        } else if (newChargeLevel.compareTo(BigDecimal.valueOf(95)) >= 0) {
            //  >= 95% → Kiểm tra sức khỏe trước khi chuyển AVAILABLE
            BigDecimal health = battery.getStateOfHealth();
            
            if (health != null && health.compareTo(MIN_HEALTH_FOR_USE) < 0) {
                // Sức khỏe thấp → Giữ CHARGING, không cho dùng
                log.warn("[Pin {}] {:.1f}% nhưng sức khỏe {:.1f}% < 70% → Tiếp tục SẠC (sẽ BẢO TRÌ ở mức 100%)",
                         battery.getId(), newChargeLevel.doubleValue(), health.doubleValue());
            } else {
                // Sức khỏe tốt → AVAILABLE nhưng VẪN SẠC tiếp đến 100%
                if (battery.getStatus() == Battery.Status.CHARGING) {
                    battery.setStatus(Battery.Status.AVAILABLE);
                    log.info(" [Battery {}]  {:.1f}% → AVAILABLE (still charging to 100%, health: {:.1f}%)",
                             battery.getId(), newChargeLevel.doubleValue(), health != null ? health.doubleValue() : 100.0);
                } else {
                    log.info(" [Pin {}] Sạc: {:.1f}% → {:.1f}% (CÓ SẴN, sạc đến 100%)",
                             battery.getId(), currentCharge.doubleValue(), newChargeLevel.doubleValue());
                }
            }
        } else {
            // < 95% → Vẫn CHARGING
            log.info(" [Pin {}] Sạc: {:.1f}% → {:.1f}% ({:.1f} giờ)",
                     battery.getId(), 
                     currentCharge.doubleValue(), 
                     newChargeLevel.doubleValue(), 
                     hoursCharged);
        }

        battery.setChargeLevel(newChargeLevel);
        
        // Reset lastChargedTime để tính tiếp từ đây
        battery.setLastChargedTime(now);
        batteryRepository.save(battery);
        
        return true;
    }

}
