package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.QuickSwapRequest;
import com.evbs.BackEndEvBs.model.response.BatteryInfoResponse;
import com.evbs.BackEndEvBs.model.response.QuickSwapPreviewResponse;
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
 * Service xử lý đổi pin nhanh qua QR Code
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuickSwapService {

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final VehicleRepository vehicleRepository;

    @Autowired
    private final BatteryRepository batteryRepository;

    @Autowired
    private final DriverSubscriptionRepository driverSubscriptionRepository;

    @Autowired
    private final SwapTransactionRepository swapTransactionRepository;

    @Autowired
    private final BatteryHealthService batteryHealthService;

    @Autowired
    private final EmailService emailService;

    @Autowired
    private final StationInventoryRepository stationInventoryRepository;

    @Autowired
    private final BookingRepository bookingRepository;

    /**
     * Preview thông tin đổi pin nhanh tại trạm
     * CHỈ HIỂN THỊ PIN MỚI SẼ ĐỔI
     */
    @Transactional(readOnly = true)
    public QuickSwapPreviewResponse previewQuickSwap(Long stationId, Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();
        
        QuickSwapPreviewResponse response = new QuickSwapPreviewResponse();
        
        // 1. Kiểm tra trạm
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));
        
        response.setStationId(station.getId());
        response.setStationName(station.getName());
        response.setStationLocation(
                station.getLocation() != null ? station.getLocation() :
                        (station.getDistrict() + ", " + station.getCity())
        );
        
        // 2. Kiểm tra xe
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));
        
        // Kiểm tra xe thuộc quyền sở hữu
        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Xe không thuộc quyền sở hữu!");
        }
        
        // Kiểm tra xe đã được phê duyệt
        if (vehicle.getStatus() != Vehicle.VehicleStatus.ACTIVE) {
            throw new IllegalStateException("Xe chưa được phê duyệt!");
        }
        
        response.setVehicleId(vehicle.getId());
        response.setVehiclePlateNumber(vehicle.getPlateNumber());
        response.setVehicleModel(vehicle.getModel());
        
        // 3. Kiểm tra loại pin tương thích
        BatteryType vehicleBatteryType = vehicle.getBatteryType();
        BatteryType stationBatteryType = station.getBatteryType();
        
        if (!vehicleBatteryType.getId().equals(stationBatteryType.getId())) {
            throw new IllegalStateException("Trạm không hỗ trợ loại pin của xe này!");
        }
        
        response.setBatteryTypeName(vehicleBatteryType.getName());
        response.setBatteryTypeCapacity(vehicleBatteryType.getCapacity());
        
        // 4. Kiểm tra subscription và lượt swap
        DriverSubscription activeSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentUser, LocalDate.now())
                .orElseThrow(() -> new IllegalStateException("Chưa có gói dịch vụ. Vui lòng mua gói!"));
        
        if (activeSubscription.getRemainingSwaps() <= 0) {
            throw new IllegalStateException("Gói đã hết lượt. Vui lòng gia hạn!");
        }
        
        response.setRemainingSwaps(activeSubscription.getRemainingSwaps());
        
        // 5. Tìm pin mới tại trạm (sẵn sàng để đổi)
        List<Battery> availableBatteries = batteryRepository.findAll()
                .stream()
                .filter(b -> b.getCurrentStation() != null
                        && b.getCurrentStation().getId().equals(station.getId())
                        && b.getBatteryType().getId().equals(vehicleBatteryType.getId())
                        && b.getStatus() == Battery.Status.AVAILABLE
                        && b.getChargeLevel().compareTo(BigDecimal.valueOf(95)) >= 0
                        && b.getStateOfHealth() != null
                        && b.getStateOfHealth().compareTo(BigDecimal.valueOf(70)) >= 0)
                .sorted((b1, b2) -> {
                    int healthCompare = b2.getStateOfHealth().compareTo(b1.getStateOfHealth());
                    if (healthCompare != 0) return healthCompare;
                    return b2.getChargeLevel().compareTo(b1.getChargeLevel());
                })
                .toList();
        
        if (availableBatteries.isEmpty()) {
            throw new NotFoundException("Trạm hiện không có pin phù hợp. Vui lòng thử lại sau!");
        }
        
        Battery newBattery = availableBatteries.get(0);
        response.setNewBatteryId(newBattery.getId());
        response.setNewBatteryModel(newBattery.getModel());
        response.setNewBatteryChargeLevel(newBattery.getChargeLevel());
        response.setNewBatteryHealth(newBattery.getStateOfHealth());
        
        return response;
    }

    /**
     * Thực hiện đổi pin nhanh
     * DRIVER TỰ ĐỔI - Không cần booking trước, đổi luôn khi quét QR
     */
    @Transactional
    public SwapTransaction executeQuickSwap(QuickSwapRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        
        log.info("Bắt đầu quick swap - Station: {}, Vehicle: {}, Driver: {}",
                request.getStationId(), request.getVehicleId(), currentUser.getId());
        
        // 1. Kiểm tra trạm
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));
        
        // 2. Kiểm tra xe
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));
        
        User driver = vehicle.getDriver();
        
        // VALIDATION: Xe phải thuộc về driver hiện tại
        if (!driver.getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Xe không thuộc quyền sở hữu!");
        }
        
        // VALIDATION: Xe phải ở trạng thái ACTIVE
        if (vehicle.getStatus() != Vehicle.VehicleStatus.ACTIVE) {
            throw new AuthenticationException("Xe chưa được phê duyệt!");
        }
        
        // VALIDATION: Xe không được có booking đang active (chưa COMPLETED hoặc CANCELLED)
        List<Booking> activeBookings = bookingRepository.findByVehicleAndStatusNotIn(
                vehicle,
                List.of(Booking.Status.COMPLETED, Booking.Status.CANCELLED)
        );
        if (!activeBookings.isEmpty()) {
            Booking activeBooking = activeBookings.get(0);
            throw new AuthenticationException(
                    "Xe đang có booking " + activeBooking.getStatus() + 
                    ". Vui lòng hoàn thành hoặc hủy booking trước khi đổi pin nhanh!"
            );
        }
        
        // 3. Kiểm tra loại pin tương thích
        BatteryType vehicleBatteryType = vehicle.getBatteryType();
        BatteryType stationBatteryType = station.getBatteryType();
        
        if (!vehicleBatteryType.getId().equals(stationBatteryType.getId())) {
            throw new AuthenticationException("Trạm không hỗ trợ loại pin của xe này!");
        }
        
        // 4. Kiểm tra subscription và lượt swap
        DriverSubscription activeSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentUser, LocalDate.now())
                .orElse(null);
        
        if (activeSubscription == null) {
            throw new AuthenticationException("Chưa có gói dịch vụ. Vui lòng mua gói!");
        }
        
        if (activeSubscription.getRemainingSwaps() <= 0) {
            throw new AuthenticationException("Gói đã hết lượt. Vui lòng gia hạn!");
        }
        
        // 5. Tìm pin cũ trên xe (nếu có)
        Battery swapInBattery = vehicle.getCurrentBattery();
        
        // 6. Lấy ĐÚNG pin đã chọn từ Preview (batteryId)
        Battery swapOutBattery = batteryRepository.findById(request.getBatteryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin với ID: " + request.getBatteryId()));
        
        // 7. VALIDATE pin đã chọn có thể đổi được không
        // 7.1. Kiểm tra pin phải ở trạm đúng
        if (swapOutBattery.getCurrentStation() == null || 
            !swapOutBattery.getCurrentStation().getId().equals(station.getId())) {
            throw new AuthenticationException("Pin không ở trạm này!");
        }
        
        // 7.2. Kiểm tra pin phải AVAILABLE
        if (swapOutBattery.getStatus() != Battery.Status.AVAILABLE) {
            throw new AuthenticationException("Pin không sẵn sàng! Trạng thái: " + swapOutBattery.getStatus());
        }
        
        // 7.3. Kiểm tra loại pin đúng
        if (!swapOutBattery.getBatteryType().getId().equals(vehicleBatteryType.getId())) {
            throw new AuthenticationException("Loại pin không tương thích!");
        }
        
        // 7.4. Kiểm tra charge đủ tiêu chuẩn (≥ 95%)
        if (swapOutBattery.getChargeLevel().compareTo(BigDecimal.valueOf(95)) < 0) {
            throw new AuthenticationException("Pin chưa đủ sạc! Hiện tại: " + swapOutBattery.getChargeLevel() + "%");
        }
        
        // 7.5. Kiểm tra health đủ tiêu chuẩn (≥ 70%)
        if (swapOutBattery.getStateOfHealth().compareTo(BigDecimal.valueOf(70)) < 0) {
            throw new AuthenticationException("Pin không đủ sức khỏe! Hiện tại: " + swapOutBattery.getStateOfHealth() + "%");
        }
        
        log.info("Sẽ đổi ĐÚNG pin đã chọn - Battery ID: {}, Charge: {}%, Health: {}%",
                swapOutBattery.getId(), swapOutBattery.getChargeLevel(), swapOutBattery.getStateOfHealth());
        
        // 8. Tạo swap transaction và LƯU SNAPSHOT TRƯỚC KHI THAY ĐỔI
        SwapTransaction transaction = new SwapTransaction();
        transaction.setDriver(driver);
        transaction.setVehicle(vehicle);
        transaction.setStation(station);
        transaction.setStaff(null); // Quick swap: Driver tự đổi, không có staff
        transaction.setSwapOutBattery(swapOutBattery);
        transaction.setSwapInBattery(swapInBattery);
        transaction.setBooking(null); // Không có booking
        transaction.setCost(BigDecimal.ZERO); // Đã trả qua subscription
        transaction.setStartTime(LocalDateTime.now());
        transaction.setEndTime(LocalDateTime.now());
        transaction.setStatus(SwapTransaction.Status.COMPLETED);
        
        // Lưu snapshot thông tin pin mới TRƯỚC KHI GIẢM
        transaction.setSwapOutBatteryModel(swapOutBattery.getModel());
        transaction.setSwapOutBatteryChargeLevel(swapOutBattery.getChargeLevel());
        transaction.setSwapOutBatteryHealth(swapOutBattery.getStateOfHealth());
        
        // Lưu snapshot thông tin pin cũ
        if (swapInBattery != null) {
            transaction.setSwapInBatteryModel(swapInBattery.getModel());
            transaction.setSwapInBatteryChargeLevel(swapInBattery.getChargeLevel());
            transaction.setSwapInBatteryHealth(swapInBattery.getStateOfHealth());
        }
        
        SwapTransaction savedTransaction = swapTransactionRepository.save(transaction);
        
        // 9. SAU KHI LƯU SNAPSHOT → Giảm pin mới xuống dưới 50%
        // (Mô phỏng việc tài xế sử dụng xe sau khi đổi pin - GIỐNG BOOKING)
        if (swapOutBattery != null) {
            java.util.Random random = new java.util.Random();
            BigDecimal randomChargeLevel = BigDecimal.valueOf(10 + random.nextInt(40)); // Random 10-49%
            swapOutBattery.setChargeLevel(randomChargeLevel);
            batteryRepository.save(swapOutBattery);
            log.info("Pin ID {} được đổi vào xe - Snapshot: {}%, Mức pin hiện tại giảm xuống: {}%",
                    swapOutBattery.getId(),
                    savedTransaction.getSwapOutBatteryChargeLevel().intValue(),
                    randomChargeLevel.intValue());
        }
        
        // 10. Xử lý hoàn chỉnh swap transaction (giống SwapTransactionService)
        handleQuickSwapCompletion(savedTransaction, activeSubscription);
        
        log.info("Quick swap hoàn tất - Transaction ID: {}", savedTransaction.getId());
        
        return savedTransaction;
    }

    // ==================== HELPER METHODS ====================
    
    /**
     * Xử lý logic hoàn chỉnh khi quick swap transaction COMPLETED
     * Học theo SwapTransactionService.handleTransactionCompletion()
     */
    private void handleQuickSwapCompletion(
            SwapTransaction transaction,
            DriverSubscription subscription
    ) {
        // 1. Xử lý pin swap (lấy staff từ transaction nếu có, nếu không dùng driver)
        User staff = transaction.getStaff();
        if (staff == null) {
            staff = transaction.getDriver(); // Quick swap: driver tự đổi
        }
        handleBatterySwap(transaction, staff);
        
        // 2. Trừ lượt swap từ subscription
        int oldRemaining = subscription.getRemainingSwaps();
        subscription.setRemainingSwaps(oldRemaining - 1);
        
        log.info("Đã trừ 1 lượt swap. Driver: {}, {} → {}",
                transaction.getDriver().getId(), oldRemaining, subscription.getRemainingSwaps());
        
        // 3. Nếu hết lượt, chuyển sang EXPIRED
        if (subscription.getRemainingSwaps() <= 0) {
            subscription.setStatus(DriverSubscription.Status.EXPIRED);
        }
        
        driverSubscriptionRepository.save(subscription);
        
        // 4. Gửi email thông báo đổi pin thành công
        try {
            emailService.sendSwapSuccessEmail(transaction.getDriver(), transaction, subscription);
            log.info("Email đổi pin thành công đã được gửi cho tài xế: {}", 
                    transaction.getDriver().getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email đổi pin thành công: {}", e.getMessage());
        }
    }
    
    /**
     * Handle battery swap logic when transaction COMPLETED
     * GIỐNG 100% SwapTransactionService.handleBatterySwap()
     */
    private void handleBatterySwap(SwapTransaction transaction, User staff) {
        Vehicle vehicle = transaction.getVehicle();
        
        // Process battery taken OUT from station (new battery for vehicle)
        if (transaction.getSwapOutBattery() != null) {
            Battery swapOutBattery = transaction.getSwapOutBattery();
            
            // Quick swap: Pin luôn là AVAILABLE, không có PENDING/reservation
            
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
            // GIỐNG BOOKING - PHẦN NÀY BỊ THIẾU TRƯỚC ĐÂY!
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

    /**
     * Lấy thông tin pin cũ của xe đang quick swap
     * Giống như /swap-transaction/old-battery nhưng dùng cho Quick Swap (không cần code)
     */
    @Transactional(readOnly = true)
    public BatteryInfoResponse getOldBatteryInfo(Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();
        
        log.info("Lấy thông tin pin cũ cho Quick Swap - Vehicle ID: {}, Driver: {}", 
                vehicleId, currentUser.getId());
        
        BatteryInfoResponse response = new BatteryInfoResponse();
        
        // 1. Kiểm tra xe
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));
        
        // 2. Kiểm tra xe thuộc quyền sở hữu
        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Xe không thuộc quyền sở hữu!");
        }
        
        // 3. Lấy pin cũ trên xe
        Battery oldBattery = vehicle.getCurrentBattery();
        
        if (oldBattery == null) {
            response.setMessage("Xe chưa có pin");
            response.setBatteryRole("OLD");
            return response;
        }
        
        // 4. Build response với thông tin pin cũ
        response.setDriverName(currentUser.getFullName());
        response.setVehiclePlate(vehicle.getPlateNumber());
        response.setBatteryRole("OLD");
        
        // Thông tin pin
        response.setBatteryId(oldBattery.getId());
        response.setModel(oldBattery.getModel());
        response.setChargeLevel(oldBattery.getChargeLevel());
        response.setStateOfHealth(oldBattery.getStateOfHealth());
        response.setStatus(oldBattery.getStatus().name());
        response.setUsageCount(oldBattery.getUsageCount());
        response.setBatteryType(oldBattery.getBatteryType().getName());
        
        response.setMessage("Thông tin pin cũ");
        
        log.info("Đã lấy thông tin pin cũ - Battery ID: {}, Charge: {}%, Health: {}%",
                oldBattery.getId(), oldBattery.getChargeLevel(), oldBattery.getStateOfHealth());
        
        return response;
    }
}
