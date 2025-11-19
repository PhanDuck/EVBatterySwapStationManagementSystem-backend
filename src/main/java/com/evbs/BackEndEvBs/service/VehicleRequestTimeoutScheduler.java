package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SCHEDULED TASK - TỰ ĐỘNG HỦY YÊU CẦU ĐĂNG KÝ XE QUÁ 12 TIẾNG
 *
 * Chức năng:
 * - Tự động chuyển xe từ PENDING -> INACTIVE nếu admin không duyệt sau 12 tiếng
 * - Gửi email thông báo cho tài xế về việc yêu cầu bị hủy do timeout
 * - Chạy mỗi 30 phút một lần
 */
@Service
@RequiredArgsConstructor
public class VehicleRequestTimeoutScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VehicleRequestTimeoutScheduler.class);

    // Thời gian chờ tối đa: 12 tiếng
    private static final long TIMEOUT_HOURS = 12;

    private final VehicleRepository vehicleRepository;
    private final EmailService emailService;

    /**
     * Chạy mỗi 30 phút (1800000 milliseconds)
     * Kiểm tra và hủy các yêu cầu đăng ký xe quá 12 tiếng
     */
    @Scheduled(fixedDelay = 1800000) // 30 phút
    @Transactional
    public void cancelExpiredVehicleRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutThreshold = now.minusHours(TIMEOUT_HOURS);

        // Tìm tất cả xe PENDING quá 12 tiếng
        List<Vehicle> expiredVehicles = vehicleRepository.findByStatusAndCreatedAtBefore(
                Vehicle.VehicleStatus.PENDING,
                timeoutThreshold
        );

        if (expiredVehicles.isEmpty()) {
            logger.debug("Không có yêu cầu đăng ký xe nào hết hạn lúc: {}", now);
            return;
        }

        logger.info("Tìm thấy {} yêu cầu đăng ký xe hết hạn (quá {} tiếng). Bắt đầu hủy...",
                expiredVehicles.size(), TIMEOUT_HOURS);

        int cancelledCount = 0;

        for (Vehicle vehicle : expiredVehicles) {
            try {
                // Kiểm tra lại status (đề phòng race condition)
                if (vehicle.getStatus() != Vehicle.VehicleStatus.PENDING) {
                    logger.debug("Xe VehicleID: {} đã được xử lý (status: {}), bỏ qua",
                            vehicle.getId(), vehicle.getStatus());
                    continue;
                }

                // Tính thời gian chờ thực tế
                long hoursWaiting = java.time.Duration.between(
                        vehicle.getCreatedAt(), now
                ).toHours();

                logger.info("Đang hủy yêu cầu đăng ký xe - VehicleID: {}, PlateNumber: {}, DriverID: {}, Đã chờ: {} giờ",
                        vehicle.getId(),
                        vehicle.getPlateNumber(),
                        vehicle.getDriver().getId(),
                        hoursWaiting);

                // Chuyển status sang INACTIVE (tự động hủy)
                vehicle.setStatus(Vehicle.VehicleStatus.INACTIVE);
                vehicle.setDeletedAt(now);
                // DeletedBy để null vì đây là hệ thống tự động hủy
                vehicle.setDeletedBy(null);

                vehicleRepository.save(vehicle);

                // Gửi email thông báo cho tài xế
                try {
                    emailService.sendVehicleTimeoutToDriver(vehicle, TIMEOUT_HOURS);
                    logger.info("Đã gửi email thông báo timeout cho tài xế: {}",
                            vehicle.getDriver().getEmail());
                } catch (Exception emailError) {
                    logger.error("Lỗi khi gửi email thông báo timeout cho tài xế. VehicleID: {}, Error: {}",
                            vehicle.getId(), emailError.getMessage());
                    // Không throw exception để tiếp tục xử lý các xe khác
                }

                cancelledCount++;
                logger.info("Đã hủy thành công yêu cầu đăng ký xe. VehicleID: {}", vehicle.getId());

            } catch (Exception e) {
                logger.error("Lỗi khi hủy yêu cầu đăng ký xe hết hạn. VehicleID: {}, Error: {}",
                        vehicle.getId(), e.getMessage(), e);
                // Tiếp tục xử lý các xe khác
            }
        }

        logger.info("Hoàn thành xử lý yêu cầu đăng ký xe hết hạn. Số lượng hủy: {}/{}",
                cancelledCount, expiredVehicles.size());
    }
}
