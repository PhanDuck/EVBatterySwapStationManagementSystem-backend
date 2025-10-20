package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.BookingRepository;
import com.evbs.BackEndEvBs.repository.DriverSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SCHEDULED TASK - AUTO-CANCEL BOOKING HET HAN
 * 
 * Chay moi 5 phut de kiem tra va huy booking qua 3 tieng:
 * 1. Tim tat ca Battery PENDING da het reservation
 * 2. Huy booking lien ket (CONFIRMED → CANCELLED)
 * 3. TRU LUOT SWAP (vi driver khong den)
 * 4. Giai phong pin (PENDING → AVAILABLE)
 */
@Service
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BookingExpirationScheduler.class);

    private final BatteryRepository batteryRepository;
    private final BookingRepository bookingRepository;
    private final DriverSubscriptionRepository driverSubscriptionRepository;

    /**
     * CHAY MOI 5 PHUT (300,000 milliseconds)
     * 
     * Tim va huy booking het han reservation
     */
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void cancelExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();
        
        // BUOC 1: TIM TAT CA PIN PENDING DA HET HAN
        List<Battery> expiredBatteries = batteryRepository.findAll()
                .stream()
                .filter(b -> b.getStatus() == Battery.Status.PENDING
                        && b.getReservationExpiry() != null
                        && b.getReservationExpiry().isBefore(now))
                .toList();
        
        if (expiredBatteries.isEmpty()) {
            logger.debug("Khong co booking nao het han luc: {}", now);
            return;
        }

        logger.info("Tim thay {} pin PENDING het han reservation. Bat dau huy booking...", expiredBatteries.size());

        int cancelledCount = 0;
        
        for (Battery battery : expiredBatteries) {
            try {
                // BUOC 2: LAY BOOKING LIEN KET
                Booking booking = battery.getReservedForBooking();
                
                if (booking == null) {
                    logger.warn("Pin PENDING nhung khong co booking lien ket. BatteryID: {}", battery.getId());
                    // Van giai phong pin
                    releaseBattery(battery);
                    continue;
                }

                // BUOC 3: HUY BOOKING (CHI HUY NEU VAN CON CONFIRMED)
                if (booking.getStatus() == Booking.Status.CONFIRMED) {
                    // TRU LUOT SWAP VI DRIVER KHONG DEN
                    deductSwapForNoShow(booking);
                    
                    booking.setStatus(Booking.Status.CANCELLED);
                    booking.setReservedBattery(null);
                    booking.setReservationExpiry(null);
                    bookingRepository.save(booking);
                    
                    logger.info("Da huy booking het han VA TRU LUOT SWAP. BookingID: {}, ConfirmationCode: {}, DriverID: {}", 
                                booking.getId(), booking.getConfirmationCode(), booking.getDriver().getId());
                    cancelledCount++;
                }

                // BUOC 4: GIAI PHONG PIN (PENDING → AVAILABLE)
                releaseBattery(battery);
                
            } catch (Exception e) {
                logger.error("Loi khi huy booking het han. BatteryID: {}", battery.getId(), e);
                // Tiep tuc xu ly cac booking khac
            }
        }

        logger.info("Hoan thanh xu ly booking het han. So luong huy: {}/{}", cancelledCount, expiredBatteries.size());
    }

    /**
     * GIAI PHONG PIN (PENDING → AVAILABLE)
     */
    private void releaseBattery(Battery battery) {
        battery.setStatus(Battery.Status.AVAILABLE);
        battery.setReservedForBooking(null);
        battery.setReservationExpiry(null);
        batteryRepository.save(battery);
        
        logger.debug("Da giai phong pin. BatteryID: {}, StationID: {}", 
                     battery.getId(), battery.getCurrentStation() != null ? battery.getCurrentStation().getId() : null);
    }

    /**
     * TRU LUOT SWAP VI DRIVER KHONG DEN
     * 
     * Tim subscription ACTIVE cua driver va tru remainingSwaps
     * Neu remainingSwaps = 0 → Doi subscription thanh EXPIRED
     */
    private void deductSwapForNoShow(Booking booking) {
        try {
            // Tim subscription ACTIVE cua driver
            List<DriverSubscription> activeSubscriptions = driverSubscriptionRepository
                    .findActiveSubscriptionsByDriver(booking.getDriver(), java.time.LocalDate.now());
            
            if (activeSubscriptions.isEmpty()) {
                logger.warn("Khong tim thay subscription ACTIVE cho driver. DriverID: {}", 
                           booking.getDriver().getId());
                return;
            }

            // Lay subscription dau tien (uu tien subscription gan het han nhat)
            DriverSubscription subscription = activeSubscriptions.get(0);
            
            int remainingBefore = subscription.getRemainingSwaps();
            
            if (remainingBefore > 0) {
                // TRU LUOT SWAP
                subscription.setRemainingSwaps(remainingBefore - 1);
                
                // Neu het luot → Doi thanh EXPIRED
                if (subscription.getRemainingSwaps() == 0) {
                    subscription.setStatus(DriverSubscription.Status.EXPIRED);
                    logger.info("Subscription het luot swap. SubscriptionID: {}, DriverID: {}", 
                               subscription.getId(), booking.getDriver().getId());
                }
                
                driverSubscriptionRepository.save(subscription);
                
                logger.info("Da tru luot swap vi khong den. DriverID: {}, RemainingSwaps: {} → {}", 
                           booking.getDriver().getId(), remainingBefore, subscription.getRemainingSwaps());
            } else {
                logger.warn("Subscription da het luot swap. SubscriptionID: {}, DriverID: {}", 
                           subscription.getId(), booking.getDriver().getId());
            }
            
        } catch (Exception e) {
            logger.error("Loi khi tru luot swap. BookingID: {}, DriverID: {}", 
                        booking.getId(), booking.getDriver().getId(), e);
        }
    }
}
