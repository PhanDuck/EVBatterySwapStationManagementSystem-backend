package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.response.BatteryInfoResponse;
import com.evbs.BackEndEvBs.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý các giao dịch hoán đổi pin
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SwapTransactionService {

    @Autowired
    private final SwapTransactionRepository swapTransactionRepository;

    @Autowired
    private final VehicleRepository vehicleRepository;

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final BatteryRepository batteryRepository;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final BookingRepository bookingRepository;

    @Autowired
    private final DriverSubscriptionRepository driverSubscriptionRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final BatteryHealthService batteryHealthService;

    @Autowired
    private final StationInventoryRepository stationInventoryRepository;

    @Autowired
    private final EmailService emailService;

    // ==================== PUBLIC METHODS ====================

    /**
     * Lấy thông tin pin CŨ (đang trên xe)
     */
    @Transactional(readOnly = true)
    public BatteryInfoResponse getOldBatteryInfoByCode(String confirmationCode) {
        log.info("Lấy thông tin pin CŨ - Mã: {}", confirmationCode);

        // Validate booking
        Booking booking = validateBookingForPreview(confirmationCode);

        Vehicle vehicle = booking.getVehicle();
        Battery oldBattery = vehicle.getCurrentBattery();

        BatteryInfoResponse response = createBaseBatteryInfoResponse(booking, "OLD");

        if (oldBattery != null) {
            mapBatteryToResponse(oldBattery, response);
            response.setMessage("Thông tin pin CŨ đang lắp trên xe");
        } else {
            response.setMessage("Xe chưa có pin. Đây là lần swap đầu tiên.");
        }

        return response;
    }

    /**
     * Lấy thông tin pin MỚI (chuẩn bị lắp)
     */
    @Transactional(readOnly = true)
    public BatteryInfoResponse getNewBatteryInfoByCode(String confirmationCode) {
        log.info("Lấy thông tin pin MỚI - Mã: {}", confirmationCode);

        // Validate booking
        Booking booking = validateBookingForPreview(confirmationCode);

        // Lấy pin đã reserve cho booking này (pin mới)
        Battery newBattery = batteryRepository.findByStatusAndReservedForBooking(
                Battery.Status.PENDING,
                booking
        ).orElseThrow(() -> new AuthenticationException(
                "Không tìm thấy pin đã đặt trước cho booking này. Vui lòng liên hệ nhân viên."
        ));

        BatteryInfoResponse response = createBaseBatteryInfoResponse(booking, "NEW");
        mapBatteryToResponse(newBattery, response);
        response.setMessage("Thông tin pin MỚI chuẩn bị lắp vào xe");

        return response;
    }

    /**
     * CREATE SWAP BY CONFIRMATION CODE - Driver tự swap tại trạm (PUBLIC)
     */
    @Transactional
    public SwapTransaction createSwapByConfirmationCode(String confirmationCode) {
        log.info("Thực hiện swap công khai - Mã xác nhận: {}", confirmationCode);

        // 1. Tìm booking bằng confirmationCode
        Booking booking = bookingRepository.findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy booking với mã: " + confirmationCode
                ));

        // 2. Lấy driver từ booking (thay vì từ authentication)
        User driver = booking.getDriver();
        log.info("Đã tìm thấy booking - ID: {}, Tài xế: {}, Xe: {}",
                booking.getId(), driver.getUsername(), booking.getVehicle().getPlateNumber());

        // 3. Validate booking status
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new AuthenticationException("Mã đã sử dụng!");
        }

        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new AuthenticationException("Booking đã bị hủy!");
        }

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new AuthenticationException("Mã chưa được kích hoạt!");
        }

        // 3.1. Double check: Nếu đã có swap transaction → Code đã dùng rồi
        SwapTransaction existingTransaction = swapTransactionRepository.findByBooking(booking)
                .orElse(null);
        if (existingTransaction != null) {
            throw new AuthenticationException("Mã đã được sử dụng!");
        }

        // 4. Validate subscription của driver
        DriverSubscription activeSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(driver, LocalDate.now())
                .orElseThrow(() -> new AuthenticationException("Không có gói dịch vụ!"));

        // KHÔNG CẦN CHECK remainingSwaps > 0 vì đã trừ lượt từ booking rồi
        // Khi swap, remainingSwaps có thể = 0 (lượt cuối) nhưng vẫn hợp lệ

        // 5. Lấy thông tin từ booking
        Vehicle vehicle = booking.getVehicle();
        Station station = booking.getStation();

        // 6. Validate battery type compatibility
        if (!station.getBatteryType().getId().equals(vehicle.getBatteryType().getId())) {
            throw new AuthenticationException("Loại pin không tương thích!");
        }

        // 7. Use RESERVED (PENDING) battery for this booking
        Battery swapOutBattery = batteryRepository.findByStatusAndReservedForBooking(
                Battery.Status.PENDING,
                booking
        ).orElseThrow(() -> new AuthenticationException("Không tìm thấy pin đặt trước!"));

        log.info("Sử dụng pin đã đặt trước {} cho booking {} (mã xác nhận: {})",
                swapOutBattery.getId(), booking.getId(), confirmationCode);

        // 8. Pin cũ của vehicle (nếu có)
        Battery swapInBattery = vehicle.getCurrentBattery();

        // 9. Lấy Staff/Admin đã confirm booking (để lưu vào SwapTransaction)
        User staffWhoConfirmed = booking.getConfirmedBy();
        if (staffWhoConfirmed == null) {
            // Fallback: Tìm admin user nếu không có staff confirmed
            staffWhoConfirmed = userRepository.findAll()
                    .stream()
                    .filter(u -> u.getRole() == User.Role.ADMIN)
                    .findFirst()
                    .orElseThrow(() -> new AuthenticationException(
                            "Lỗi hệ thống: Không tìm thấy nhân viên xác nhận booking"
                    ));
            log.warn("Sử dụng tài khoản admin thay thế cho xác nhận booking: {}", staffWhoConfirmed.getUsername());
        }

        // 10. Tạo swap transaction và LƯU SNAPSHOT TRƯỚC KHI GIẢM PIN
        // (Để history hiển thị: pin cũ = thấp, pin mới = cao)
        SwapTransaction transaction = new SwapTransaction();
        transaction.setDriver(driver);
        transaction.setVehicle(vehicle);
        transaction.setStation(station);
        transaction.setStaff(staffWhoConfirmed);
        transaction.setSwapOutBattery(swapOutBattery);
        transaction.setSwapInBattery(swapInBattery);
        transaction.setBooking(booking);
        transaction.setCost(BigDecimal.ZERO);  // Đã trả qua subscription
        transaction.setStartTime(LocalDateTime.now());
        transaction.setEndTime(LocalDateTime.now());
        transaction.setStatus(SwapTransaction.Status.COMPLETED);


        // LƯU SNAPSHOT thông tin pin TRƯỚC KHI GIẢM (để history đúng)
        if (swapOutBattery != null) {
            transaction.setSwapOutBatteryModel(swapOutBattery.getModel());
            transaction.setSwapOutBatteryChargeLevel(swapOutBattery.getChargeLevel()); // Pin mới: CAO
            transaction.setSwapOutBatteryHealth(swapOutBattery.getStateOfHealth());
        }
        if (swapInBattery != null) {
            transaction.setSwapInBatteryModel(swapInBattery.getModel());
            transaction.setSwapInBatteryChargeLevel(swapInBattery.getChargeLevel()); // Pin cũ: THẤP
            transaction.setSwapInBatteryHealth(swapInBattery.getStateOfHealth());
        }

        SwapTransaction savedTransaction = swapTransactionRepository.save(transaction);

        // 11. SAU KHI LƯU SNAPSHOT → Giảm pin mới xuống dưới 50%
        // (Mô phỏng việc tài xế sử dụng xe sau khi đổi pin)
        if (swapOutBattery != null) {
            java.util.Random random = new java.util.Random();
            BigDecimal randomChargeLevel = BigDecimal.valueOf(10 + random.nextInt(40)); // Random 10-49%
            swapOutBattery.setChargeLevel(randomChargeLevel);
            batteryRepository.save(swapOutBattery);
            log.info("🔋 Pin ID {} được đổi vào xe - Snapshot: {}%, Mức pin hiện tại giảm xuống: {}%",
                    swapOutBattery.getId(),
                    savedTransaction.getSwapOutBatteryChargeLevel().intValue(),
                    randomChargeLevel.intValue());
        }

        // 12. Xử lý hoàn tất: pin, subscription, booking
        handleTransactionCompletion(savedTransaction, activeSubscription, booking);

        log.info("Self-service swap hoàn thành thành công - Tài xế: {}, Mã: {}, Nhân viên: {}, Xe: {}",
                driver.getUsername(), confirmationCode, staffWhoConfirmed.getUsername(), vehicle.getPlateNumber());

        return savedTransaction;
    }

    // ==================== DRIVER METHODS ====================

    /**
     * READ - Lấy transactions của driver hiện tại
     */
    @Transactional(readOnly = true)
    public List<SwapTransaction> getMyTransactions() {
        User currentUser = authenticationService.getCurrentUser();
        // Sử dụng JOIN FETCH để tránh N+1 query problem
        return swapTransactionRepository.findByDriverWithDetails(currentUser);
    }

    /**
     * READ - Lấy transaction cụ thể của driver
     */
    @Transactional(readOnly = true)
    public SwapTransaction getMyTransaction(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        // Sử dụng JOIN FETCH để tránh N+1 query problem
        return swapTransactionRepository.findByIdAndDriverWithDetails(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));
    }

    // ==================== ADMIN/STAFF METHODS ====================

    /**
     * READ - Lấy tất cả transactions (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<SwapTransaction> getAllSwapTransactions() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Không có quyền truy cập!");
        }
        // Sử dụng JOIN FETCH để tránh N+1 query problem
        return swapTransactionRepository.findAllWithDetails();
    }

    /**
     * XEM LỊCH SỬ ĐỔI PIN CỦA XE
     */
    @Transactional(readOnly = true)
    public List<SwapTransaction> getVehicleSwapHistory(Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();

        // Tìm xe
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe với ID: " + vehicleId));

        // Kiểm tra quyền:
        // - Driver chỉ xem được xe của mình
        // - Staff/Admin xem được tất cả
        if (currentUser.getRole() == User.Role.DRIVER) {
            if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
                throw new AuthenticationException("Không có quyền xem!");
            }
        }

        // Lấy tất cả swap transactions của xe, sắp xếp mới nhất trước
        // Sử dụng JOIN FETCH để tránh N+1 query problem
        List<SwapTransaction> history = swapTransactionRepository.findByVehicleWithDetailsOrderByStartTimeDesc(vehicle);

        log.info("Đã lấy {} giao dịch swap cho xe {}", history.size(), vehicleId);

        return history;
    }

    /**
     * XEM LỊCH SỬ SỬ DỤNG CỦA PIN
     */
    @Transactional(readOnly = true)
    public List<SwapTransaction> getBatteryUsageHistory(Long batteryId) {
        User currentUser = authenticationService.getCurrentUser();

        // Chỉ Staff/Admin mới xem được lịch sử pin
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Chỉ Staff/Admin!");
        }

        // Kiểm tra pin có tồn tại không
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin với ID: " + batteryId));

        // Lấy tất cả lần pin được SWAP OUT (lấy ra từ trạm) với JOIN FETCH
        List<SwapTransaction> swapOutHistory = swapTransactionRepository.findBySwapOutBatteryWithDetailsOrderByStartTimeDesc(battery);

        // Lấy tất cả lần pin được SWAP IN (đem vào trạm) với JOIN FETCH
        List<SwapTransaction> swapInHistory = swapTransactionRepository.findBySwapInBatteryWithDetailsOrderByStartTimeDesc(battery);

        // Gộp 2 danh sách và sắp xếp theo thời gian
        List<SwapTransaction> allHistory = new java.util.ArrayList<>();
        allHistory.addAll(swapOutHistory);
        allHistory.addAll(swapInHistory);

        // Sắp xếp theo startTime từ mới đến cũ
        allHistory.sort((t1, t2) -> t2.getStartTime().compareTo(t1.getStartTime()));

        log.info("Đã lấy {} giao dịch swap cho pin {} (model: {})",
                allHistory.size(), batteryId, battery.getModel());

        return allHistory;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Validate booking cho preview
     */
    private Booking validateBookingForPreview(String confirmationCode) {
        Booking booking = bookingRepository.findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy booking với mã: " + confirmationCode
                ));

        // Validate booking status
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new AuthenticationException("Mã xác nhận đã được sử dụng. Booking này đã hoàn thành.");
        }

        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new AuthenticationException("Mã xác nhận không còn hiệu lực. Booking đã bị hủy.");
        }

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new AuthenticationException(
                    "Mã xác nhận chưa được kích hoạt. Trạng thái hiện tại: " + booking.getStatus()
            );
        }

        // Kiểm tra nếu đã có swap transaction
        SwapTransaction existingTransaction = swapTransactionRepository.findByBooking(booking)
                .orElse(null);
        if (existingTransaction != null) {
            throw new AuthenticationException("Mã xác nhận đã được sử dụng. Không thể xem thông tin.");
        }

        return booking;
    }

    /**
     * Tạo base response
     */
    private BatteryInfoResponse createBaseBatteryInfoResponse(Booking booking, String batteryRole) {
        User driver = booking.getDriver();
        Vehicle vehicle = booking.getVehicle();
        Station station = booking.getStation();

        BatteryInfoResponse response = new BatteryInfoResponse();
        response.setConfirmationCode(booking.getConfirmationCode());
        response.setDriverName(driver.getFullName());
        response.setVehiclePlate(vehicle.getPlateNumber());
        response.setStationName(station.getName());
        response.setBatteryRole(batteryRole);

        return response;
    }

    /**
     * Map battery information to response
     */
    private void mapBatteryToResponse(Battery battery, BatteryInfoResponse response) {
        response.setBatteryId(battery.getId());
        response.setModel(battery.getModel());
        response.setChargeLevel(battery.getChargeLevel());
        response.setStateOfHealth(battery.getStateOfHealth());
        response.setStatus(battery.getStatus().toString());
        response.setUsageCount(battery.getUsageCount());

        if (battery.getBatteryType() != null) {
            response.setBatteryType(battery.getBatteryType().getName());
        }
    }

    /**
     * Xử lý logic hoàn chỉnh khi swap transaction COMPLETED
     */
    private void handleTransactionCompletion(
            SwapTransaction transaction,
            DriverSubscription subscription,
            Booking booking
    ) {
        // 1. Xử lý pin (lưu thông tin staff thực hiện)
        User currentStaff = transaction.getStaff(); // Lấy từ transaction (đã set trong createSwapByConfirmationCode)
        handleBatterySwap(transaction, currentStaff);

        // 2. KHÔNG TRỪ remainingSwaps Ở ĐÂY NỮA (đã trừ từ booking)
        // Logic mới: Booking đã trừ lượt swap, swap chỉ thực hiện đổi pin

        // 3. Cập nhật booking: set status COMPLETED và XÓA confirmationCode để tái sử dụng
        if (booking != null && booking.getStatus() == Booking.Status.CONFIRMED) {
            booking.setStatus(Booking.Status.COMPLETED);
            booking.setConfirmationCode(null); // Xóa mã để có thể random mã mới dễ dàng hơn
            bookingRepository.save(booking);
            log.info("Đã xóa confirmationCode cho booking ID {} sau khi swap thành công", booking.getId());
        }

        // 4. Kiểm tra nếu hết lượt swap → set subscription status = EXPIRED
        if (subscription.getRemainingSwaps() <= 0) {
            subscription.setStatus(DriverSubscription.Status.EXPIRED);
            driverSubscriptionRepository.save(subscription);
        }

        // 5. Gửi email thông báo đổi pin thành công
        try {
            emailService.sendSwapSuccessEmail(transaction.getDriver(), transaction, subscription);
            log.info("Email đổi pin thành công đã được gửi cho tài xế: {}", transaction.getDriver().getEmail());
        } catch (Exception emailException) {
            log.error("Lỗi khi gửi email đổi pin thành công: {}", emailException.getMessage());
        }
    }

    /**
     * Handle battery swap logic when transaction COMPLETED
     */
    private void handleBatterySwap(SwapTransaction transaction, User staff) {
        Vehicle vehicle = transaction.getVehicle();

        // Process battery taken OUT from station (new battery for vehicle)
        if (transaction.getSwapOutBattery() != null) {
            Battery swapOutBattery = transaction.getSwapOutBattery();

            // Clear reservation if battery was PENDING (reserved for booking)
            if (swapOutBattery.getStatus() == Battery.Status.PENDING) {
                swapOutBattery.setReservedForBooking(null);
                swapOutBattery.setReservationExpiry(null);
                log.info("Đã xóa đặt trước cho pin {}", swapOutBattery.getId());
            }

            swapOutBattery.setCurrentStation(null); // No longer at any station
            swapOutBattery.setStatus(Battery.Status.IN_USE); // Now in use

            batteryRepository.save(swapOutBattery);

            // Check and degrade SOH after usage
            batteryHealthService.degradeSOHAfterUsage(swapOutBattery);

            log.info("Đã xử lý SWAP_OUT cho pin {}", swapOutBattery.getId());
        }

        // Process battery brought IN to station (old battery from vehicle)
        if (transaction.getSwapInBattery() != null) {
            Battery swapInBattery = transaction.getSwapInBattery();
            swapInBattery.setCurrentStation(transaction.getStation()); // Assign to station

            // Check battery health first: If health < 70% -> MAINTENANCE
            BigDecimal health = swapInBattery.getStateOfHealth();
            if (health != null && health.compareTo(BigDecimal.valueOf(70)) < 0) {
                swapInBattery.setStatus(Battery.Status.MAINTENANCE);
                swapInBattery.setLastChargedTime(null);
                log.warn("Pin swap-in {} có sức khỏe thấp {}% < 70%, đặt thành MAINTENANCE",
                        swapInBattery.getId(), health.doubleValue());
            } else {
                // Good health, check charge level
                BigDecimal currentCharge = swapInBattery.getChargeLevel();
                if (currentCharge != null && currentCharge.compareTo(BigDecimal.valueOf(100)) < 0) {
                    swapInBattery.setStatus(Battery.Status.CHARGING); // Start charging
                    swapInBattery.setLastChargedTime(LocalDateTime.now());
                } else {
                    swapInBattery.setStatus(Battery.Status.AVAILABLE); // Fully charged, ready to use
                }
            }

            batteryRepository.save(swapInBattery);

            // XÓA khỏi StationInventory nếu có (vì pin đã về trạm, không còn ở kho)
            stationInventoryRepository.findByBattery(swapInBattery).ifPresent(inventory -> {
                stationInventoryRepository.delete(inventory);
                log.info("Đã xóa pin {} khỏi StationInventory (pin đã về trạm)", swapInBattery.getId());
            });

            log.info("Đã xử lý SWAP_IN cho pin {}", swapInBattery.getId());
        }

        // Update vehicle current battery
        // Mount new battery (swapOut) on vehicle, replacing old battery (swapIn)
        if (transaction.getSwapOutBattery() != null) {
            vehicle.setCurrentBattery(transaction.getSwapOutBattery());
            vehicleRepository.save(vehicle);
            log.info("Đã cập nhật currentBattery cho xe {} từ {} sang {}",
                    vehicle.getId(),
                    transaction.getSwapInBattery() != null ? transaction.getSwapInBattery().getId() : "null",
                    transaction.getSwapOutBattery().getId());
        }
    }

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}