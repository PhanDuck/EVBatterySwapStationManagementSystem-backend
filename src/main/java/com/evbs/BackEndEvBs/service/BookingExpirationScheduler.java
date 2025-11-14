package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.model.EmailDetail;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SCHEDULED TASK - AUTO-CANCEL BOOKING HET HAN
 */
@Service
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BookingExpirationScheduler.class);

    private final BatteryRepository batteryRepository;
    private final BookingRepository bookingRepository;
    private final DriverSubscriptionRepository driverSubscriptionRepository;
    private final EmailService emailService;

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void cancelExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();

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
                Booking booking = battery.getReservedForBooking();

                if (booking == null) {
                    logger.warn("Pin PENDING nhung khong co booking lien ket. BatteryID: {}", battery.getId());
                    releaseBattery(battery);
                    continue;
                }

                if (booking.getStatus() == Booking.Status.CONFIRMED) {
                    // KHÔNG HOÀN LẠI LƯỢT SWAP (đã trừ từ lúc booking, driver không đến = mất lượt)

                    // Lưu mã code trước khi xóa để gửi email
                    String oldCode = booking.getConfirmationCode();

                    booking.setStatus(Booking.Status.CANCELLED);
                    booking.setConfirmationCode(null); // Xóa mã code để giải phóng
                    booking.setReservedBattery(null);
                    booking.setReservationExpiry(null);
                    bookingRepository.save(booking);

                    logger.info("Da huy booking het han (KHONG HOAN LAI LUOT). BookingID: {}, ConfirmationCode: '{}' (da xoa), DriverID: {}",
                            booking.getId(), oldCode, booking.getDriver().getId());
                    cancelledCount++;

                    // GỬI EMAIL THÔNG BÁO HỦY TỰ ĐỘNG CHO DRIVER
                    sendAutoCancellationEmail(booking, oldCode);
                }

                releaseBattery(battery);

            } catch (Exception e) {
                logger.error("Loi khi huy booking het han. BatteryID: {}", battery.getId(), e);
            }
        }

        logger.info("Hoan thanh xu ly booking het han. So luong huy: {}/{}", cancelledCount, expiredBatteries.size());
    }

    private void releaseBattery(Battery battery) {
        battery.setStatus(Battery.Status.AVAILABLE);
        battery.setReservedForBooking(null);
        battery.setReservationExpiry(null);
        batteryRepository.save(battery);

        logger.debug("Da giai phong pin. BatteryID: {}, StationID: {}",
                battery.getId(), battery.getCurrentStation() != null ? battery.getCurrentStation().getId() : null);
    }

    /**
     * GỬI EMAIL THÔNG BÁO HỦY TỰ ĐỘNG CHO DRIVER
     * Vì booking bị hủy do hết thời gian reservation (3 tiếng)
     */
    private void sendAutoCancellationEmail(Booking booking, String confirmationCode) {
        try {
            EmailDetail emailDetail = new EmailDetail();
            emailDetail.setRecipient(booking.getDriver().getEmail());
            emailDetail.setSubject("THÔNG BÁO HỦY BOOKING TỰ ĐỘNG - " + confirmationCode);
            emailDetail.setFullName(booking.getDriver().getFullName());

            emailDetail.setBookingId(booking.getId());
            emailDetail.setStationName(booking.getStation().getName());
            emailDetail.setStationLocation(
                    booking.getStation().getLocation() != null ? booking.getStation().getLocation() :
                            (booking.getStation().getDistrict() + ", " + booking.getStation().getCity())
            );
            emailDetail.setStationContact(booking.getStation().getContactInfo() != null ? booking.getStation().getContactInfo() : "Chưa cập nhật");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            emailDetail.setBookingTime(booking.getBookingTime().format(formatter));

            // TÁCH RIÊNG: Model xe và Biển số xe
            emailDetail.setVehicleModel(booking.getVehicle().getModel() != null ? booking.getVehicle().getModel() : "Xe điện");
            emailDetail.setVehiclePlateNumber(booking.getVehicle().getPlateNumber()); // Biển số riêng
            
            emailDetail.setBatteryType(
                    booking.getStation().getBatteryType().getName() +
                            (booking.getStation().getBatteryType().getCapacity() != null ? " - " + booking.getStation().getBatteryType().getCapacity() + "kWh" : "")
            );
            emailDetail.setStatus("CANCELLED");
            emailDetail.setConfirmationCode(confirmationCode);

            // Thông báo lý do hủy tự động
            emailDetail.setCancellationPolicy(
                    "Booking của bạn đã bị hủy tự động do hết thời gian giữ chỗ (3 giờ). " +
                            "Lượt swap đã bị trừ từ lúc booking và KHÔNG ĐƯỢC HOÀN LẠI vì bạn không đến."
            );

            // Thêm loại hủy và lý do
            emailDetail.setCancellationType("AUTO");
            emailDetail.setCancellationReason("Hết thời gian giữ chỗ (3 giờ)");

            emailService.sendBookingCancellationEmail(emailDetail);

            logger.info("Da gui email thong bao huy tu dong cho driver. BookingID: {}, Driver: {}",
                    booking.getId(), booking.getDriver().getEmail());

        } catch (Exception e) {
            logger.error("Loi khi gui email thong bao huy tu dong. BookingID: {}", booking.getId(), e);
        }
    }
}