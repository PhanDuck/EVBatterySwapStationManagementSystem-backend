package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.SwapTransactionRequest;
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
 * 
 * LOGIC QUAN TRỌNG:
 * - Khi swap transaction được COMPLETED (hoàn thành):
 *   + Pin swapOut (được đem ra khỏi trạm): currentStation = null, status = IN_USE
 *   + Pin swapIn (được đem vào trạm): currentStation = station, status = AVAILABLE
 * 
 * - Điều này đảm bảo:
 *   + Pin được lấy ra sẽ không còn tính vào capacity của trạm
 *   + Trạm sẽ có chỗ trống để nhận pin mới
 *   + Pin đang được sử dụng bên ngoài có thể được track
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

    /**
     * CREATE - Tạo transaction mới (Driver)
     */
    @Transactional
    public SwapTransaction createTransaction(SwapTransactionRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        // VALIDATION: Check if driver has ACTIVE subscription with remainingSwaps > 0
        DriverSubscription activeSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentUser, LocalDate.now())
                .orElseThrow(() -> new AuthenticationException(
                        "You must have an active subscription with remaining swaps to create a swap transaction."
                ));

        if (activeSubscription.getRemainingSwaps() <= 0) {
            throw new AuthenticationException("You have no remaining swaps in your subscription.");
        }

        // Validate station first (cần station để tìm booking)
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Station not found"));

        // AUTO FIND CONFIRMED BOOKING (no need to pass bookingId)
        // Tìm booking CONFIRMED gần nhất của driver tại station này
        Booking booking = bookingRepository.findLatestConfirmedBooking(currentUser, station)
                .orElse(null);  // Có thể null nếu driver chưa booking (walk-in customer)

        // Validate vehicle thuộc về driver
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));

        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Vehicle does not belong to current user");
        }

        // VALIDATION: Check if station supports vehicle's battery type
        if (!station.getBatteryType().getId().equals(vehicle.getBatteryType().getId())) {
            throw new AuthenticationException(
                    "INCOMPATIBLE! Station '" + station.getName() + 
                    "' only supports battery type '" + station.getBatteryType().getName() + 
                    "', but vehicle '" + vehicle.getPlateNumber() + 
                    "' requires battery type '" + vehicle.getBatteryType().getName() + "'." +
                    "\n\nPlease go to another station!"
            );
        }

        // Station đã được validate ở trên (để tìm booking)

        // Validate staff
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        if (staff.getRole() != User.Role.STAFF && staff.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("User is not staff or admin");
        }

        // BATTERY SELECTION LOGIC
        Battery swapOutBattery = null;  // Battery OUT from station (new battery for driver)
        Battery swapInBattery = null;   // Battery IN to station (old battery from driver)

        // SWAP OUT: Get battery for driver
        if (request.getSwapOutBatteryId() != null) {
            // Manual selection by staff
            swapOutBattery = batteryRepository.findById(request.getSwapOutBatteryId())
                    .orElseThrow(() -> new NotFoundException("Swap-out battery not found"));
        } else if (booking != null) {
            // If has CONFIRMED booking, use the RESERVED (PENDING) battery
            swapOutBattery = batteryRepository.findByStatusAndReservedForBooking(
                    Battery.Status.PENDING, 
                    booking
            ).orElseThrow(() -> new AuthenticationException(
                    "No reserved battery found for this booking. Please contact staff."
            ));
            
            log.info("Using reserved battery {} for booking {}", swapOutBattery.getId(), booking.getId());
        } else {
            // Walk-in customer (no booking) - find best AVAILABLE battery
            List<Battery> availableBatteries = batteryRepository
                    .findAvailableBatteriesAtStation(
                            station.getId(), 
                            Battery.Status.AVAILABLE, 
                            BigDecimal.valueOf(80.0)  // Minimum 80% charge
                    );
            
            if (availableBatteries.isEmpty()) {
                throw new AuthenticationException(
                        "No available batteries with charge >= 80% at this station. " +
                        "Please book in advance or choose another station."
                );
            }
            
            swapOutBattery = availableBatteries.get(0);  // Take battery with highest charge
            log.info("Walk-in customer - using available battery {}", swapOutBattery.getId());
        }

        // SWAP IN: Battery brought IN to station (old battery from driver's vehicle)
        if (request.getSwapInBatteryId() != null) {
            // Manual selection
            swapInBattery = batteryRepository.findById(request.getSwapInBatteryId())
                    .orElseThrow(() -> new NotFoundException("Swap-in battery not found"));
        } else {
            // Auto selection: Find CURRENT battery being used by vehicle
            // Tìm swap transaction gần nhất của vehicle để biết pin nào đang trên xe
            SwapTransaction lastSwap = swapTransactionRepository
                    .findTopByVehicleOrderByStartTimeDesc(vehicle)
                    .orElse(null);
            
            if (lastSwap != null && lastSwap.getSwapOutBattery() != null) {
                // Lấy pin từ lần swap trước (pin đang trên xe)
                swapInBattery = lastSwap.getSwapOutBattery();
            } else {
                // Xe chưa từng swap, không có pin để trả
                // Cho phép swap mà không cần trả pin (first time swap)
                swapInBattery = null;
                log.warn("Vehicle {} has no battery to return (first swap or no history)", vehicle.getId());
            }
        }

        // Create transaction manually to avoid ModelMapper conflicts
        SwapTransaction transaction = new SwapTransaction();
        transaction.setDriver(currentUser);
        transaction.setVehicle(vehicle);
        transaction.setStation(station);
        transaction.setStaff(staff);
        transaction.setSwapOutBattery(swapOutBattery);
        transaction.setSwapInBattery(swapInBattery);
        transaction.setBooking(booking);
        transaction.setStartTime(LocalDateTime.now());
        if (request.getCost() != null) {
            transaction.setCost(request.getCost());
        }
        // status = PENDING_PAYMENT (default)
        
        // Save snapshot of battery info at swap time
        if (swapOutBattery != null) {
            transaction.setSwapOutBatteryModel(swapOutBattery.getModel());
            transaction.setSwapOutBatteryChargeLevel(swapOutBattery.getChargeLevel());
            transaction.setSwapOutBatteryHealth(swapOutBattery.getStateOfHealth());
        }
        if (swapInBattery != null) {
            transaction.setSwapInBatteryModel(swapInBattery.getModel());
            transaction.setSwapInBatteryChargeLevel(swapInBattery.getChargeLevel());
            transaction.setSwapInBatteryHealth(swapInBattery.getStateOfHealth());
        }

        SwapTransaction savedTransaction = swapTransactionRepository.save(transaction);

        // Nếu transaction được tạo với status COMPLETED, xử lý logic pin và subscription
        if (SwapTransaction.Status.COMPLETED.equals(savedTransaction.getStatus())) {
            handleTransactionCompletion(savedTransaction, activeSubscription, booking);
        }

        return savedTransaction;
    }

    /**
     * READ - Lấy transactions của driver hiện tại
     */
    @Transactional(readOnly = true)
    public List<SwapTransaction> getMyTransactions() {
        User currentUser = authenticationService.getCurrentUser();
        return swapTransactionRepository.findByDriver(currentUser);
    }

    /**
     * READ - Lấy transaction cụ thể của driver
     */
    @Transactional(readOnly = true)
    public SwapTransaction getMyTransaction(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        return swapTransactionRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }

    /**
     * UPDATE - Hoàn thành transaction (Driver)
     */
    @Transactional
    public SwapTransaction completeMyTransaction(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        SwapTransaction transaction = swapTransactionRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        // Lấy active subscription
        DriverSubscription activeSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentUser, LocalDate.now())
                .orElseThrow(() -> new AuthenticationException("No active subscription found"));

        transaction.setStatus(SwapTransaction.Status.COMPLETED);
        transaction.setEndTime(LocalDateTime.now());

        // Xử lý logic hoàn thành: pin, subscription, booking
        handleTransactionCompletion(transaction, activeSubscription, transaction.getBooking());

        return swapTransactionRepository.save(transaction);
    }

    /**
     * READ - Lấy tất cả transactions (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<SwapTransaction> getAllTransactions() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return swapTransactionRepository.findAll();
    }

    /**
     * UPDATE - Cập nhật transaction (Admin/Staff only)
     */
    @Transactional
    public SwapTransaction updateTransaction(Long id, SwapTransactionRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        SwapTransaction transaction = swapTransactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        // Update các field
        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new NotFoundException("Vehicle not found"));
            transaction.setVehicle(vehicle);
        }

        if (request.getStationId() != null) {
            Station station = stationRepository.findById(request.getStationId())
                    .orElseThrow(() -> new NotFoundException("Station not found"));
            transaction.setStation(station);
        }

        if (request.getStaffId() != null) {
            User staff = userRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new NotFoundException("Staff not found"));
            transaction.setStaff(staff);
        }

        if (request.getCost() != null) {
            transaction.setCost(request.getCost());
        }

        return swapTransactionRepository.save(transaction);
    }

    /**
     * UPDATE - Cập nhật status transaction (Admin/Staff only)
     */
    @Transactional
    public SwapTransaction updateTransactionStatus(Long id, SwapTransaction.Status status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        SwapTransaction transaction = swapTransactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        transaction.setStatus(status);

        if (SwapTransaction.Status.COMPLETED.equals(status) && transaction.getEndTime() == null) {
            transaction.setEndTime(LocalDateTime.now());
        }

        // Logic: Khi hoàn thành swap, xử lý pin, subscription, booking
        if (SwapTransaction.Status.COMPLETED.equals(status)) {
            // Lấy subscription của driver
            DriverSubscription activeSubscription = driverSubscriptionRepository
                    .findActiveSubscriptionByDriver(transaction.getDriver(), LocalDate.now())
                    .orElseThrow(() -> new AuthenticationException("Driver has no active subscription"));

            handleTransactionCompletion(transaction, activeSubscription, transaction.getBooking());
        }

        return swapTransactionRepository.save(transaction);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Xử lý logic hoàn chỉnh khi swap transaction COMPLETED:
     * 1. Xử lý pin (swapOut và swapIn)
     * 2. Trừ remainingSwaps trong subscription
     * 3. Auto-complete booking nếu có
     * 4. Check và expire subscription nếu hết lượt
     */
    private void handleTransactionCompletion(
            SwapTransaction transaction,
            DriverSubscription subscription,
            Booking booking
    ) {
        // 1. Xử lý pin (lưu thông tin staff thực hiện)
        User currentStaff = authenticationService.getCurrentUser();
        handleBatterySwap(transaction, currentStaff);

        // 2. Trừ remainingSwaps
        int currentRemaining = subscription.getRemainingSwaps();
        subscription.setRemainingSwaps(currentRemaining - 1);
        driverSubscriptionRepository.save(subscription);

        // 3. Auto-complete booking nếu có
        if (booking != null && booking.getStatus() == Booking.Status.CONFIRMED) {
            booking.setStatus(Booking.Status.COMPLETED);
            bookingRepository.save(booking);
        }

        // 4. Kiểm tra nếu hết lượt swap → set subscription status = EXPIRED
        if (subscription.getRemainingSwaps() <= 0) {
            subscription.setStatus(DriverSubscription.Status.EXPIRED);
            driverSubscriptionRepository.save(subscription);
        }
    }

    /**
     * Handle battery swap logic when transaction COMPLETED
     * - SwapOut battery: currentStation = null, status = IN_USE, mounted on vehicle
     * - SwapIn battery: currentStation = station, status = AVAILABLE/CHARGING/MAINTENANCE
     * - Vehicle.currentBattery: Update from swapIn to swapOut
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
                log.info("Cleared reservation for battery {}", swapOutBattery.getId());
            }
            
            swapOutBattery.setCurrentStation(null); // No longer at any station
            swapOutBattery.setStatus(Battery.Status.IN_USE); // Now in use
            
            // Increase usage count
            Integer currentUsage = swapOutBattery.getUsageCount();
            swapOutBattery.setUsageCount(currentUsage != null ? currentUsage + 1 : 1);
            
            batteryRepository.save(swapOutBattery);
            
            // Check and degrade SOH after usage
            batteryHealthService.degradeSOHAfterUsage(swapOutBattery);
            
            log.info("Processed SWAP_OUT for battery {}", swapOutBattery.getId());
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
                log.warn("Swap-in battery {} has low health {}% < 70%, set to MAINTENANCE", 
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
            
            log.info("Processed SWAP_IN for battery {}", swapInBattery.getId());
        }
        
        // Update vehicle current battery
        // Mount new battery (swapOut) on vehicle, replacing old battery (swapIn)
        if (transaction.getSwapOutBattery() != null) {
            vehicle.setCurrentBattery(transaction.getSwapOutBattery());
            vehicleRepository.save(vehicle);
            log.info("Updated vehicle {} currentBattery from {} to {}", 
                    vehicle.getId(), 
                    transaction.getSwapInBattery() != null ? transaction.getSwapInBattery().getId() : "null",
                    transaction.getSwapOutBattery().getId());
        }
    }

    /**
     * ⭐ CREATE SWAP BY CONFIRMATION CODE - Driver tự swap tại trạm
     * 
     * Driver nhập confirmationCode vào máy tại trạm
     * → Hệ thống tự động:
     *   1. Verify booking CONFIRMED
     *   2. Lấy thông tin vehicle, station từ booking
     *   3. Chọn pin tốt nhất
     *   4. Thực hiện swap
     *   5. Trừ remainingSwaps
     *   6. Complete booking
     */
    @Transactional
    public SwapTransaction createSwapByConfirmationCode(String confirmationCode) {
        User currentDriver = authenticationService.getCurrentUser();

        // 1. Tìm booking bằng confirmationCode
        Booking booking = bookingRepository.findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new NotFoundException(
                        "❌ Không tìm thấy booking với mã: " + confirmationCode
                ));

        // 2. Validate booking - Code chỉ dùng 1 lần
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new AuthenticationException(
                    "❌ Mã xác nhận đã được sử dụng. Booking này đã hoàn thành."
            );
        }
        
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new AuthenticationException(
                    "❌ Mã xác nhận không còn hiệu lực. Booking đã bị hủy."
            );
        }
        
        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new AuthenticationException(
                    "❌ Mã xác nhận chưa được kích hoạt. Vui lòng chờ staff xác nhận. " +
                    "Trạng thái hiện tại: " + booking.getStatus()
            );
        }

        // 2.1. Double check: Nếu đã có swap transaction → Code đã dùng rồi
        SwapTransaction existingTransaction = swapTransactionRepository.findByBooking(booking)
                .orElse(null);
        if (existingTransaction != null) {
            throw new AuthenticationException(
                    "❌ Mã xác nhận đã được sử dụng lúc " + 
                    existingTransaction.getEndTime() + ". Không thể swap lại."
            );
        }

        // 3. Kiểm tra driver có phải owner của booking không
        if (!booking.getDriver().getId().equals(currentDriver.getId())) {
            throw new AuthenticationException(
                    "❌ Booking này không thuộc về bạn"
            );
        }

        // 4. Validate subscription
        DriverSubscription activeSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentDriver, LocalDate.now())
                .orElseThrow(() -> new AuthenticationException(
                        "❌ Bạn không có subscription ACTIVE"
                ));

        if (activeSubscription.getRemainingSwaps() <= 0) {
            throw new AuthenticationException("❌ Bạn đã hết lượt swap");
        }

        // 5. Lấy thông tin từ booking
        Vehicle vehicle = booking.getVehicle();
        Station station = booking.getStation();

        // 6. Validate battery type compatibility
        if (!station.getBatteryType().getId().equals(vehicle.getBatteryType().getId())) {
            throw new AuthenticationException(
                    "❌ KHÔNG TƯƠNG THÍCH! Trạm '" + station.getName() + 
                    "' chỉ hỗ trợ pin loại '" + station.getBatteryType().getName() + 
                    "', nhưng xe '" + vehicle.getPlateNumber() + 
                    "' cần pin loại '" + vehicle.getBatteryType().getName() + "'."
            );
        }

        // 7. Use RESERVED (PENDING) battery for this booking
        Battery swapOutBattery = batteryRepository.findByStatusAndReservedForBooking(
                Battery.Status.PENDING, 
                booking
        ).orElseThrow(() -> new AuthenticationException(
                "No reserved battery found for this booking. Please contact staff."
        ));
        
        log.info("Using reserved battery {} for booking {} (confirmation code: {})", 
                 swapOutBattery.getId(), booking.getId(), confirmationCode);

        // 8. Pin cũ của vehicle (nếu có)
        Battery swapInBattery = vehicle.getCurrentBattery();

        // 9. Lấy Staff/Admin đã confirm booking (để lưu vào SwapTransaction)
        User staffWhoConfirmed = booking.getConfirmedBy();
        if (staffWhoConfirmed == null) {
            throw new AuthenticationException(
                    "❌ Lỗi hệ thống: Booking đã CONFIRMED nhưng không có thông tin người confirm"
            );
        }

        // 10. Tạo swap transaction
        SwapTransaction transaction = new SwapTransaction();
        transaction.setDriver(currentDriver);
        transaction.setVehicle(vehicle);
        transaction.setStation(station);
        transaction.setStaff(staffWhoConfirmed);  // ⭐ Dùng Staff đã confirm booking
        transaction.setSwapOutBattery(swapOutBattery);
        transaction.setSwapInBattery(swapInBattery);
        transaction.setBooking(booking);
        transaction.setCost(BigDecimal.ZERO);  // Đã trả qua subscription
        transaction.setStartTime(LocalDateTime.now());
        transaction.setEndTime(LocalDateTime.now());
        transaction.setStatus(SwapTransaction.Status.COMPLETED);
        
        // ⭐ LƯU SNAPSHOT thông tin pin tại thời điểm swap
        if (swapOutBattery != null) {
            transaction.setSwapOutBatteryModel(swapOutBattery.getModel());
            transaction.setSwapOutBatteryChargeLevel(swapOutBattery.getChargeLevel());
            transaction.setSwapOutBatteryHealth(swapOutBattery.getStateOfHealth());
        }
        if (swapInBattery != null) {
            transaction.setSwapInBatteryModel(swapInBattery.getModel());
            transaction.setSwapInBatteryChargeLevel(swapInBattery.getChargeLevel());
            transaction.setSwapInBatteryHealth(swapInBattery.getStateOfHealth());
        }

        SwapTransaction savedTransaction = swapTransactionRepository.save(transaction);

        // 11. Xử lý hoàn tất: pin, subscription, booking
        handleTransactionCompletion(savedTransaction, activeSubscription, booking);

        log.info("✅ Self-service swap completed by driver {} with code {} (confirmed by staff {})", 
                currentDriver.getUsername(), confirmationCode, staffWhoConfirmed.getUsername());

        return savedTransaction;
    }

    /**
     * ⭐ XEM LỊCH SỬ ĐỔI PIN CỦA XE
     * 
     * Trả về danh sách tất cả các lần đổi pin của 1 xe cụ thể
     * Sắp xếp theo thời gian mới nhất đến cũ nhất
     * 
     * @param vehicleId ID của xe
     * @return List<SwapTransaction>
     */
    @Transactional(readOnly = true)
    public List<SwapTransaction> getVehicleSwapHistory(Long vehicleId) {
        User currentUser = authenticationService.getCurrentUser();
        
        // Tìm xe
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("❌ Không tìm thấy xe với ID: " + vehicleId));
        
        // Kiểm tra quyền:
        // - Driver chỉ xem được xe của mình
        // - Staff/Admin xem được tất cả
        if (currentUser.getRole() == User.Role.DRIVER) {
            if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
                throw new AuthenticationException("❌ Bạn không có quyền xem lịch sử xe này");
            }
        }
        
        // Lấy tất cả swap transactions của xe, sắp xếp mới nhất trước
        List<SwapTransaction> history = swapTransactionRepository.findByVehicleOrderByStartTimeDesc(vehicle);
        
        log.info("📜 Retrieved {} swap transactions for vehicle {}", history.size(), vehicleId);
        
        return history;
    }

    /**
     * 🔋 XEM LỊCH SỬ SỬ DỤNG CỦA PIN
     * 
     * Xem pin đã được dùng bởi những driver/xe nào, tại trạm nào
     * Bao gồm cả lần pin được lấy ra (swapOut) và đem vào (swapIn)
     * 
     * @param batteryId ID của pin
     * @return List<SwapTransaction> - Lịch sử tất cả giao dịch liên quan đến pin
     */
    @Transactional(readOnly = true)
    public List<SwapTransaction> getBatteryUsageHistory(Long batteryId) {
        User currentUser = authenticationService.getCurrentUser();
        
        // Chỉ Staff/Admin mới xem được lịch sử pin
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("❌ Chỉ Staff/Admin mới có quyền xem lịch sử sử dụng pin");
        }
        
        // Kiểm tra pin có tồn tại không
        Battery battery = batteryRepository.findById(batteryId)
                .orElseThrow(() -> new NotFoundException("❌ Không tìm thấy pin với ID: " + batteryId));
        
        // Lấy tất cả lần pin được SWAP OUT (lấy ra từ trạm)
        List<SwapTransaction> swapOutHistory = swapTransactionRepository.findBySwapOutBatteryOrderByStartTimeDesc(battery);
        
        // Lấy tất cả lần pin được SWAP IN (đem vào trạm)
        List<SwapTransaction> swapInHistory = swapTransactionRepository.findBySwapInBatteryOrderByStartTimeDesc(battery);
        
        // Gộp 2 danh sách và sắp xếp theo thời gian
        List<SwapTransaction> allHistory = new java.util.ArrayList<>();
        allHistory.addAll(swapOutHistory);
        allHistory.addAll(swapInHistory);
        
        // Sắp xếp theo startTime từ mới đến cũ
        allHistory.sort((t1, t2) -> t2.getStartTime().compareTo(t1.getStartTime()));
        
        log.info("🔋 Retrieved {} swap transactions for battery {} (model: {})", 
                allHistory.size(), batteryId, battery.getModel());
        
        return allHistory;
    }

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}