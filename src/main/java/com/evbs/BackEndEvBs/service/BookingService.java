package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.EmailDetail;
import com.evbs.BackEndEvBs.model.request.BookingRequest;
import com.evbs.BackEndEvBs.repository.BatteryRepository;
import com.evbs.BackEndEvBs.repository.BookingRepository;
import com.evbs.BackEndEvBs.repository.DriverSubscriptionRepository;
import com.evbs.BackEndEvBs.repository.StaffStationAssignmentRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import com.evbs.BackEndEvBs.repository.UserRepository;
import com.evbs.BackEndEvBs.util.ConfirmationCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private final BookingRepository bookingRepository;

    @Autowired
    private final VehicleRepository vehicleRepository;

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final DriverSubscriptionRepository driverSubscriptionRepository;

    @Autowired
    private final BatteryRepository batteryRepository;

    @Autowired
    private final StaffStationAssignmentRepository staffStationAssignmentRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private final UserRepository userRepository;

    // Cấu hình thời gian cho phép hủy booking (phút) - TRƯỚC 1 TIẾNG
    private static final int ALLOW_CANCEL_BEFORE_MINUTES = 60;

    /**
     * CREATE - Tao booking moi (Driver) - TỰ ĐỘNG SET THỜI GIAN 3 TIẾNG SAU
     */
    @Transactional
    public Booking createBooking(BookingRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        // VALIDATION: BAT BUOC phai co subscription ACTIVE VA CON LUOT SWAP
        DriverSubscription activeSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentUser, LocalDate.now())
                .orElse(null);
        
        // Kiểm tra có subscription và còn lượt swap
        if (activeSubscription == null) {
            throw new AuthenticationException("Chưa có gói dịch vụ. Vui lòng mua gói!");
        }
        
        if (activeSubscription.getRemainingSwaps() <= 0) {
            throw new AuthenticationException("Gói đã hết lượt. Vui lòng gia hạn!");
        }

        // Cho phép driver có nhiều xe booking cùng lúc

        // VALIDATION: Max 10 bookings per user per day
        LocalDate today = LocalDate.now();
        long bookingsToday = bookingRepository.findByDriverWithDetails(currentUser)
                .stream()
                .filter(b -> b.getBookingTime() != null && b.getBookingTime().toLocalDate().isEqual(today))
                .count();
        if (bookingsToday >= 10) {
            throw new AuthenticationException("Đã đạt giới hạn 10 lượt/ngày!");
        }

        // Validate vehicle thuộc về driver
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));

        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Xe không thuộc quyền sở hữu!");
        }

        // VALIDATION: Xe phải ở trạng thái ACTIVE mới được booking
        if (vehicle.getStatus() != Vehicle.VehicleStatus.ACTIVE) {
            throw new AuthenticationException("Xe chưa được phê duyệt!");
        }

        // KIỂM TRA: 1 xe chỉ được có 1 booking active tại 1 thời điểm
        List<Booking> vehicleActiveBookings = bookingRepository.findByVehicleAndStatusNotIn(
                vehicle,
                List.of(Booking.Status.CANCELLED, Booking.Status.COMPLETED)
        );

        if (!vehicleActiveBookings.isEmpty()) {
            throw new AuthenticationException("Xe đã có booking. Vui lòng hoàn tất hoặc hủy trước!");
        }

        // Validate station
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));

        // Validate station có cùng loại pin với xe
        if (!station.getBatteryType().getId().equals(vehicle.getBatteryType().getId())) {
            throw new AuthenticationException("Trạm không hỗ trợ loại pin này!");
        }

        // ========== TỰ ĐỘNG SET THỜI GIAN 3 TIẾNG SAU ==========
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime bookingTime = now.plusHours(3); // LUÔN ĐẶT 3 TIẾNG SAU

        // ========== TỰ ĐỘNG CONFIRM BOOKING VÀ RESERVE PIN ==========

        // Tìm pin available
        BatteryType requiredBatteryType = vehicle.getBatteryType();

        List<Battery> availableBatteries = batteryRepository.findAll()
                .stream()
                .filter(b -> b.getCurrentStation() != null
                        && b.getCurrentStation().getId().equals(station.getId())
                        && b.getBatteryType().getId().equals(requiredBatteryType.getId())
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
            throw new NotFoundException("Trạm hết pin. Vui lòng chọn trạm khác!");
        }

        // Lấy pin có sức khỏe cao nhất
        Battery reservedBattery = availableBatteries.get(0);

        // Reserve pin (status = PENDING, khóa trong 3 tiếng)
        LocalDateTime expiryTime = bookingTime; // HẾT HẠN ĐÚNG VÀO GIỜ BOOKING

        reservedBattery.setStatus(Battery.Status.PENDING);
        reservedBattery.setReservedForBooking(null);
        reservedBattery.setReservationExpiry(expiryTime);
        batteryRepository.save(reservedBattery);

        // Generate code ngay khi tạo booking
        String confirmationCode = ConfirmationCodeGenerator.generateUnique(
                10,
                code -> bookingRepository.findByConfirmationCode(code).isPresent()
        );

        // Tạo booking với status CONFIRMED ngay lập tức
        Booking booking = new Booking();
        booking.setDriver(currentUser);
        booking.setVehicle(vehicle);
        booking.setStation(station);
        booking.setBookingTime(bookingTime); // SET THỜI GIAN 3 TIẾNG SAU
        booking.setCreatedAt(now);

        // SET CONFIRMED NGAY LẬP TỨC
        booking.setConfirmationCode(confirmationCode);
        booking.setStatus(Booking.Status.CONFIRMED);
        booking.setReservedBattery(reservedBattery);
        booking.setReservationExpiry(expiryTime);

        // Tự động set system user hoặc null cho confirmedBy
        try {
            userRepository.findAll()
                    .stream()
                    .filter(u -> u.getRole() == User.Role.ADMIN)
                    .findFirst()
                    .ifPresent(booking::setConfirmedBy);
        } catch (Exception ignore) {}

        Booking savedBooking = bookingRepository.save(booking);

        // Cập nhật battery với booking đã lưu
        reservedBattery.setReservedForBooking(savedBooking);
        batteryRepository.save(reservedBattery);

        // TRỪ LƯỢT SWAP NGAY KHI BOOKING THÀNH CÔNG
        int currentRemaining = activeSubscription.getRemainingSwaps();
        activeSubscription.setRemainingSwaps(currentRemaining - 1);
        driverSubscriptionRepository.save(activeSubscription);
        
        log.info("Đã trừ 1 lượt swap khi booking. Driver: {}, Còn lại: {}", 
                currentUser.getId(), activeSubscription.getRemainingSwaps());

        // Gửi email xác nhận booking VỚI MÃ CODE
        sendBookingConfirmedEmail(savedBooking, booking.getConfirmedBy());

        return savedBooking;
    }

    /**
     * Hủy booking (Driver) - CHỈ CHO HỦY TRƯỚC 1 TIẾNG SO VỚI GIỜ BOOKING
     */
    @Transactional
    public Booking cancelMyBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        Booking booking = bookingRepository.findByIdAndDriverWithDetails(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đặt chỗ"));

        LocalDateTime now = LocalDateTime.now();

        // TÍNH THỜI GIAN CÒN LẠI ĐẾN GIỜ BOOKING
        long minutesUntilBooking = Duration.between(now, booking.getBookingTime()).toMinutes();

        // QUAN TRỌNG: CHỈ CHO PHÉP HỦY KHI CÒN TRÊN 1 TIẾNG
        if (minutesUntilBooking <= ALLOW_CANCEL_BEFORE_MINUTES) {
            throw new AuthenticationException("Quá gần giờ đặt! Liên hệ staff để hủy.");
        }

        // Kiểm tra nếu booking đã COMPLETED hoặc CANCELLED
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new AuthenticationException("Booking đã hoàn thành!");
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new AuthenticationException("Booking đã bị hủy!");
        }

        // NẾU BOOKING ĐÃ CONFIRMED VÀ CÓ PIN RESERVED → GIẢI PHÓNG PIN
        if (booking.getStatus() == Booking.Status.CONFIRMED && booking.getReservedBattery() != null) {
            Battery battery = booking.getReservedBattery();

            // Giải phóng pin (PENDING → AVAILABLE)
            if (battery.getStatus() == Battery.Status.PENDING) {
                battery.setStatus(Battery.Status.AVAILABLE);
                battery.setReservedForBooking(null);
                battery.setReservationExpiry(null);
                batteryRepository.save(battery);

                System.out.println(String.format(
                        "Driver hủy booking. BookingID: %d, DriverID: %d, BatteryID: %d đã giải phóng",
                        booking.getId(), currentUser.getId(), battery.getId()
                ));
            }

            // Clear booking reservation
            booking.setReservedBattery(null);
            booking.setReservationExpiry(null);
        }

        // HOÀN LẠI LƯỢT SWAP TRƯỚC KHI HỦY BOOKING
        List<DriverSubscription> subscriptions = driverSubscriptionRepository.findByDriver_Id(currentUser.getId());
        DriverSubscription subscription = subscriptions.stream()
                .filter(s -> s.getStatus() == DriverSubscription.Status.ACTIVE || s.getStatus() == DriverSubscription.Status.EXPIRED)
                .max((s1, s2) -> s1.getId().compareTo(s2.getId()))
                .orElse(null);
        
        if (subscription != null) {
            int oldRemaining = subscription.getRemainingSwaps();
            subscription.setRemainingSwaps(oldRemaining + 1);
            
            // Nếu subscription đã EXPIRED nhưng còn hoàn lại lượt, kích hoạt lại thành ACTIVE
            if (subscription.getStatus() == DriverSubscription.Status.EXPIRED && subscription.getRemainingSwaps() > 0) {
                subscription.setStatus(DriverSubscription.Status.ACTIVE);
            }
            
            driverSubscriptionRepository.save(subscription);
            
            log.info("Đã hoàn lại 1 lượt swap khi hủy booking. Driver: {}, {} → {}, Status: {}", 
                    currentUser.getId(), oldRemaining, subscription.getRemainingSwaps(), subscription.getStatus());
        } else {
            log.warn("KHÔNG TÌM THẤY subscription để hoàn lại lượt cho driver: {}", currentUser.getId());
        }

        // Hủy booking và xóa confirmation code để giải phóng mã
        booking.setStatus(Booking.Status.CANCELLED);
        booking.setConfirmationCode(null); // Xóa mã code để giải phóng
        Booking savedBooking = bookingRepository.save(booking);

        // Gửi email thông báo hủy booking
        sendBookingCancellationEmail(savedBooking, "DRIVER", null);

        // Detach entity to prevent lazy loading
        savedBooking.setReservedBattery(null);

        return savedBooking;
    }

    /**
     * Hủy booking bởi Staff/Admin (vẫn cho phép hủy mọi lúc)
     */
    @Transactional
    public Booking cancelBookingByStaff(Long id, String reason) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Chỉ Staff/Admin mới được hủy!");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay booking voi ID: " + id));

        // STAFF CHỈ CANCEL ĐƯỢC BOOKING CỦA TRẠM MÌNH QUẢN LÝ
        if (currentUser.getRole() == User.Role.STAFF) {
            Station bookingStation = booking.getStation();
            if (bookingStation == null) {
                throw new AuthenticationException("Booking không có trạm!");
            }

            if (!staffStationAssignmentRepository.existsByStaffAndStation(currentUser, bookingStation)) {
                throw new AuthenticationException("Bạn không quản lý trạm này!");
            }
        }

        // Kiểm tra: Không cho hủy booking đã COMPLETED hoặc CANCELLED
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new AuthenticationException("Booking đã hoàn tất!");
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new AuthenticationException("Booking đã bị hủy!");
        }

        // NẾU BOOKING ĐÃ CONFIRMED VÀ CÓ PIN RESERVED → GIẢI PHÓNG PIN
        if (booking.getStatus() == Booking.Status.CONFIRMED && booking.getReservedBattery() != null) {
            Battery battery = booking.getReservedBattery();

            if (battery.getStatus() == Battery.Status.PENDING) {
                battery.setStatus(Battery.Status.AVAILABLE);
                battery.setReservedForBooking(null);
                battery.setReservationExpiry(null);
                batteryRepository.save(battery);

                System.out.println(String.format(
                        "Nhân viên đã hủy đơn đặt chỗ ở trạng thái XÁC NHẬN. Mã đơn: %d, Mã nhân viên: %d, Lý do: %s, Pin có mã %d đã được giải phóng.",
                        booking.getId(), currentUser.getId(),
                        reason != null ? reason : "Không có lí do",
                        battery.getId()
                ));
            }

            booking.setReservedBattery(null);
            booking.setReservationExpiry(null);
        }

        // HOÀN LẠI LƯỢT SWAP TRƯỚC KHI STAFF HỦY BOOKING
        User driver = booking.getDriver();
        // Tìm subscription mới nhất (kể cả EXPIRED) để hoàn lại lượt
        List<DriverSubscription> subscriptions = driverSubscriptionRepository.findByDriver_Id(driver.getId());
        DriverSubscription subscription = subscriptions.stream()
                .filter(s -> s.getStatus() == DriverSubscription.Status.ACTIVE || s.getStatus() == DriverSubscription.Status.EXPIRED)
                .max((s1, s2) -> s1.getId().compareTo(s2.getId()))
                .orElse(null);
        
        if (subscription != null) {
            int oldRemaining = subscription.getRemainingSwaps();
            subscription.setRemainingSwaps(oldRemaining + 1);
            
            // Nếu subscription đã EXPIRED nhưng còn hoàn lại lượt, kích hoạt lại thành ACTIVE
            if (subscription.getStatus() == DriverSubscription.Status.EXPIRED && subscription.getRemainingSwaps() > 0) {
                subscription.setStatus(DriverSubscription.Status.ACTIVE);
            }
            
            driverSubscriptionRepository.save(subscription);
            
            log.info("Đã hoàn lại 1 lượt swap khi staff hủy booking. Driver: {}, {} → {}, Status: {}", 
                    driver.getId(), oldRemaining, subscription.getRemainingSwaps(), subscription.getStatus());
        } else {
            log.warn("KHÔNG TÌM THẤY subscription để hoàn lại lượt cho driver: {}", driver.getId());
        }

        // Hủy booking
        booking.setStatus(Booking.Status.CANCELLED);
        booking.setConfirmationCode(null); // Xóa mã code để giải phóng
        booking.setCancellationReason(reason); // Lưu lý do hủy

        System.out.println(String.format(
                "Nhân viên đã hủy đơn đặt chỗ. Mã đơn: %d, Mã tài xế: %d, Mã nhân viên: %d, Lý do: %s",
                booking.getId(), booking.getDriver().getId(), currentUser.getId(),
                reason != null ? reason : "Không có lí do"
        ));

        // Gửi email thông báo hủy booking
        sendBookingCancellationEmail(booking, "STAFF", reason);

        return bookingRepository.save(booking);
    }

    /**
     * Gửi email xác nhận booking đã được CONFIRMED với confirmation code
     */
    private void sendBookingConfirmedEmail(Booking booking, User confirmedBy) {
        try {
            User driver = booking.getDriver();
            Vehicle vehicle = booking.getVehicle();
            Station station = booking.getStation();

            EmailDetail emailDetail = new EmailDetail();
            emailDetail.setRecipient(driver.getEmail());
            emailDetail.setSubject("ĐẶT LỊCH THÀNH CÔNG - Mã đổi pin: " + booking.getConfirmationCode());
            emailDetail.setFullName(driver.getFullName());

            emailDetail.setBookingId(booking.getId());
            emailDetail.setStationName(station.getName());
            emailDetail.setStationLocation(
                    station.getLocation() != null ? station.getLocation() :
                            (station.getDistrict() + ", " + station.getCity())
            );
            emailDetail.setStationContact(station.getContactInfo() != null ? station.getContactInfo() : "Chưa cập nhật");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            emailDetail.setBookingTime(booking.getBookingTime().format(formatter));

            // TÁCH RIÊNG: Model xe và Biển số xe
            emailDetail.setVehicleModel(vehicle.getModel() != null ? vehicle.getModel() : "Xe điện");
            emailDetail.setVehiclePlateNumber(vehicle.getPlateNumber()); //Biển số riêng

            emailDetail.setBatteryType(
                    station.getBatteryType().getName() +
                            (station.getBatteryType().getCapacity() != null ? " - " + station.getBatteryType().getCapacity() + "kWh" : "")
            );
            emailDetail.setStatus(booking.getStatus().toString());

            // Mã xác nhận KHÔNG kèm biển số nữa
            emailDetail.setConfirmationCode(booking.getConfirmationCode());
            emailDetail.setConfirmedBy(confirmedBy != null ? confirmedBy.getFullName() : "Hệ thống");

            // Thêm thông tin về chính sách hủy booking
            emailDetail.setCancellationPolicy(
                    String.format("Lưu ý: Bạn chỉ có thể hủy đơn đặt chỗ trước %d phút so với thời gian đã đặt. Sau thời gian này, vui lòng liên hệ nhân viên để được hỗ trợ.", ALLOW_CANCEL_BEFORE_MINUTES)
            );

            emailService.sendBookingConfirmedEmail(emailDetail);
        } catch (Exception e) {
            System.err.println("Gửi email xác nhận đơn đặt chỗ thất bại: " + e.getMessage());
        }
    }

    /**
     * Gửi email thông báo hủy booking
     */
    private void sendBookingCancellationEmail(Booking booking, String cancellationType, String cancellationReason) {
        try {
            User driver = booking.getDriver();
            Vehicle vehicle = booking.getVehicle();
            Station station = booking.getStation();

            EmailDetail emailDetail = new EmailDetail();
            emailDetail.setRecipient(driver.getEmail());
            emailDetail.setSubject("THÔNG BÁO HỦY ĐẶT LỊCH");
            emailDetail.setFullName(driver.getFullName());

            emailDetail.setBookingId(booking.getId());
            emailDetail.setStationName(station.getName());
            emailDetail.setStationLocation(
                    station.getLocation() != null ? station.getLocation() :
                            (station.getDistrict() + ", " + station.getCity())
            );
            emailDetail.setStationContact(station.getContactInfo() != null ? station.getContactInfo() : "Chưa cập nhật");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            emailDetail.setBookingTime(booking.getBookingTime().format(formatter));

            // TÁCH RIÊNG: Model xe và Biển số xe
            emailDetail.setVehicleModel(vehicle.getModel() != null ? vehicle.getModel() : "Xe điện");
            emailDetail.setVehiclePlateNumber(vehicle.getPlateNumber()); // Biển số riêng

            emailDetail.setBatteryType(
                    station.getBatteryType().getName() +
                            (station.getBatteryType().getCapacity() != null ? " - " + station.getBatteryType().getCapacity() + "kWh" : "")
            );
            emailDetail.setStatus("HỦY");
            // Mã xác nhận KHÔNG kèm biển số
            emailDetail.setConfirmationCode(booking.getConfirmationCode());

            // Thêm thông tin chính sách hủy
            emailDetail.setCancellationPolicy("Lịch đặt của bạn đã được hủy thành công. Pin đã được giải phóng.");

            // Thêm loại hủy và lý do hủy
            emailDetail.setCancellationType(cancellationType);
            emailDetail.setCancellationReason(cancellationReason);

            emailService.sendBookingCancellationEmail(emailDetail);
        } catch (Exception e) {
            System.err.println("Gửi email xác nhận đơn đặt chỗ thất bại: " + e.getMessage());
        }
    }

    // ==================== CÁC METHOD KHÁC ====================

    @Transactional(readOnly = true)
    public List<Booking> getMyBookings() {
        User currentUser = authenticationService.getCurrentUser();
        // Sử dụng JOIN FETCH để tránh N+1 query problem
        return bookingRepository.findByDriverWithDetails(currentUser);
    }

    @Transactional(readOnly = true)
    public Booking getMyBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        // Sử dụng JOIN FETCH để tránh N+1 query problem
        return bookingRepository.findByIdAndDriverWithDetails(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Lịch đặt không tìm thấy"));
    }

    @Transactional(readOnly = true)
    public List<Station> getCompatibleStations(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("xe không tìm thấy"));
        return stationRepository.findStationsWithAvailableBatteries(
                vehicle.getBatteryType(),
                80
        );
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Không có quyền truy cập!");
        }

        if (currentUser.getRole() == User.Role.ADMIN) {
            // Sử dụng JOIN FETCH để tránh N+1 query problem
            return bookingRepository.findAllWithDetails();
        }

        List<Station> myStations = staffStationAssignmentRepository.findStationsByStaff(currentUser);
        if (myStations.isEmpty()) {
            return List.of();
        }

        // Sử dụng JOIN FETCH để tránh N+1 query problem
        return bookingRepository.findByStationInWithDetails(myStations);
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsForMyStations() {
        User currentUser = authenticationService.getCurrentUser();

        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Chỉ Staff/Admin mới xem được!");
        }

        if (currentUser.getRole() == User.Role.ADMIN) {
            // Sử dụng JOIN FETCH để tránh N+1 query problem
            return bookingRepository.findAllWithDetails();
        }

        List<Station> myStations = staffStationAssignmentRepository.findStationsByStaff(currentUser);

        if (myStations.isEmpty()) {
            throw new AuthenticationException("Chưa được phân công vào trạm!");
        }

        // Sử dụng JOIN FETCH để tránh N+1 query problem
        return bookingRepository.findByStationInWithDetails(myStations);
    }



    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}