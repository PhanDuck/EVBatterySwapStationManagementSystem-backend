package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.QuickSwapRequest;
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
            response.setCanSwap(false);
            response.setMessage("Xe chưa được phê duyệt!");
            return response;
        }
        
        response.setVehicleId(vehicle.getId());
        response.setVehiclePlateNumber(vehicle.getPlateNumber());
        response.setVehicleModel(vehicle.getModel());
        
        // 3. Kiểm tra loại pin tương thích
        BatteryType vehicleBatteryType = vehicle.getBatteryType();
        BatteryType stationBatteryType = station.getBatteryType();
        
        if (!vehicleBatteryType.getId().equals(stationBatteryType.getId())) {
            response.setCanSwap(false);
            response.setMessage("Trạm không hỗ trợ loại pin của xe này!");
            return response;
        }
        
        response.setBatteryTypeName(vehicleBatteryType.getName());
        response.setBatteryTypeCapacity(vehicleBatteryType.getCapacity());
        
        // 4. Kiểm tra subscription và lượt swap
        DriverSubscription activeSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentUser, LocalDate.now())
                .orElse(null);
        
        if (activeSubscription == null) {
            response.setCanSwap(false);
            response.setMessage("Chưa có gói dịch vụ. Vui lòng mua gói!");
            return response;
        }
        
        if (activeSubscription.getRemainingSwaps() <= 0) {
            response.setCanSwap(false);
            response.setMessage("Gói đã hết lượt. Vui lòng gia hạn!");
            return response;
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
            response.setCanSwap(false);
            response.setMessage("Trạm hiện không có pin phù hợp. Vui lòng thử lại sau!");
            return response;
        }
        
        Battery newBattery = availableBatteries.get(0);
        response.setNewBatteryId(newBattery.getId());
        response.setNewBatteryModel(newBattery.getModel());
        response.setNewBatteryChargeLevel(newBattery.getChargeLevel());
        response.setNewBatteryHealth(newBattery.getStateOfHealth());
        
        // 6. Sẵn sàng đổi pin
        response.setCanSwap(true);
        response.setMessage("Sẵn sàng đổi pin!");
        
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
        
        // 5. VALIDATION: Max 10 swaps per user per day (giống booking)
        LocalDate today = LocalDate.now();
        long swapsToday = swapTransactionRepository.findByDriver(currentUser)
                .stream()
                .filter(t -> t.getStartTime() != null && 
                            t.getStartTime().toLocalDate().isEqual(today) &&
                            t.getStatus() == SwapTransaction.Status.COMPLETED)
                .count();
        if (swapsToday >= 10) {
            throw new AuthenticationException("Đã đạt giới hạn 10 lượt đổi pin/ngày!");
        }
        
        // 6. Tìm pin cũ trên xe (nếu có)
        Battery swapInBattery = vehicle.getCurrentBattery();
        
        // 7. Lấy ĐÚNG pin đã chọn từ Preview (batteryId)
        Battery swapOutBattery = batteryRepository.findById(request.getBatteryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin với ID: " + request.getBatteryId()));
        
        // 8. VALIDATE pin này có thể đổi được không
        // Kiểm tra pin phải ở trạm đúng
        if (swapOutBattery.getCurrentStation() == null || 
            !swapOutBattery.getCurrentStation().getId().equals(station.getId())) {
            throw new AuthenticationException("Pin không ở trạm này!");
        }
        
        // Kiểm tra pin phải AVAILABLE
        if (swapOutBattery.getStatus() != Battery.Status.AVAILABLE) {
            throw new AuthenticationException("Pin không sẵn sàng! Trạng thái: " + swapOutBattery.getStatus());
        }
        
        // Kiểm tra loại pin đúng
        if (!swapOutBattery.getBatteryType().getId().equals(vehicleBatteryType.getId())) {
            throw new AuthenticationException("Loại pin không tương thích!");
        }
        
        // Kiểm tra charge và health đủ tiêu chuẩn
        if (swapOutBattery.getChargeLevel().compareTo(BigDecimal.valueOf(95)) < 0) {
            throw new AuthenticationException("Pin chưa đủ sạc! Hiện tại: " + swapOutBattery.getChargeLevel() + "%");
        }
        
        if (swapOutBattery.getStateOfHealth().compareTo(BigDecimal.valueOf(70)) < 0) {
            throw new AuthenticationException("Pin không đủ sức khỏe! Hiện tại: " + swapOutBattery.getStateOfHealth() + "%");
        }
        
        log.info("Sẽ đổi ĐÚNG pin đã chọn - Battery ID: {}, Charge: {}%, Health: {}%",
                swapOutBattery.getId(), swapOutBattery.getChargeLevel(), swapOutBattery.getStateOfHealth());
        
        // 9. Tạo swap transaction và LƯU SNAPSHOT TRƯỚC KHI THAY ĐỔI
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
        
        // 10. Xử lý pin mới (lên xe)
        swapOutBattery.setCurrentStation(null);
        swapOutBattery.setStatus(Battery.Status.IN_USE);
        batteryRepository.save(swapOutBattery);
        
        // Kiểm tra và giảm SOH sau khi sử dụng
        batteryHealthService.degradeSOHAfterUsage(swapOutBattery);
        
        log.info("Pin mới {} đã lên xe {}", swapOutBattery.getId(), vehicle.getPlateNumber());
        
        // 11. Cập nhật xe: gắn pin mới
        vehicle.setCurrentBattery(swapOutBattery);
        vehicleRepository.save(vehicle);
        
        // 12. Xử lý pin cũ (về trạm nếu có)
        if (swapInBattery != null) {
            swapInBattery.setCurrentStation(station);
            swapInBattery.setStatus(Battery.Status.CHARGING);
            
            // Giảm charge level xuống 20-40%
            BigDecimal newChargeLevel = BigDecimal.valueOf(20 + (Math.random() * 20));
            swapInBattery.setChargeLevel(newChargeLevel);
            
            batteryRepository.save(swapInBattery);
            
            log.info("Pin cũ {} đã về trạm {}", swapInBattery.getId(), station.getName());
        }
        
        // 13. Trừ lượt swap
        int oldRemaining = activeSubscription.getRemainingSwaps();
        activeSubscription.setRemainingSwaps(oldRemaining - 1);
        
        // Nếu hết lượt, chuyển sang EXPIRED
        if (activeSubscription.getRemainingSwaps() <= 0) {
            activeSubscription.setStatus(DriverSubscription.Status.EXPIRED);
        }
        
        driverSubscriptionRepository.save(activeSubscription);
        
        log.info("Đã trừ 1 lượt swap. Driver: {}, {} → {}, Status: {}",
                currentUser.getId(), oldRemaining, activeSubscription.getRemainingSwaps(),
                activeSubscription.getStatus());
        
        // 14. Gửi email thông báo
        sendQuickSwapSuccessEmail(savedTransaction, activeSubscription);
        
        log.info("Quick swap hoàn tất - Transaction ID: {}", savedTransaction.getId());
        
        return savedTransaction;
    }

    /**
     * Gửi email thông báo đổi pin thành công
     */
    private void sendQuickSwapSuccessEmail(SwapTransaction transaction, DriverSubscription subscription) {
        try {
            emailService.sendSwapSuccessEmail(transaction, subscription);
            log.info("Đã gửi email thông báo đổi pin thành công");
        } catch (Exception e) {
            log.error("Lỗi khi gửi email: {}", e.getMessage());
        }
    }
}
