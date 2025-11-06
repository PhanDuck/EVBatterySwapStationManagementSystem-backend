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

        // VALIDATION 0: BAT BUOC phai co subscription ACTIVE
        DriverSubscription activeSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentUser, LocalDate.now())
                .orElseThrow(() -> new AuthenticationException(
                        "BAT BUOC: Ban phai mua goi dich vu truoc khi booking. " +
                                "Vui long mua ServicePackage truoc."
                ));

        // VALIDATION 1: Kiem tra con luot swap khong
        if (activeSubscription.getRemainingSwaps() <= 0) {
            throw new AuthenticationException(
                    "Goi dich vu cua ban da het luot swap. " +
                            "Vui long gia han hoac mua goi moi."
            );
        }

        // ❌ XÓA: Không check driver có booking active (cho phép driver có nhiều xe booking cùng lúc)
        // Driver có 2 xe → được booking 2 lần cho 2 xe khác nhau

        // VALIDATION: Max 10 bookings per user per day
        LocalDate today = LocalDate.now();
        long bookingsToday = bookingRepository.findByDriver(currentUser)
                .stream()
                .filter(b -> b.getBookingTime() != null && b.getBookingTime().toLocalDate().isEqual(today))
                .count();
        if (bookingsToday >= 10) {
            throw new AuthenticationException("Bạn đã đạt tối đa 10 lượt đặt chỗ trong ngày hôm nay.");
        }

        // Validate vehicle thuộc về driver
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));

        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Xe không thuộc sở hữu của người dùng hiện tại");
        }

        // KIỂM TRA: 1 xe chỉ được có 1 booking active tại 1 thời điểm
        List<Booking> vehicleActiveBookings = bookingRepository.findByVehicleAndStatusNotIn(
                vehicle,
                List.of(Booking.Status.CANCELLED, Booking.Status.COMPLETED)
        );

        if (!vehicleActiveBookings.isEmpty()) {
            Booking existingBooking = vehicleActiveBookings.get(0);
            throw new AuthenticationException(
                    String.format("Xe này đã có đặt chỗ đang hoạt động (ID: %d, Trạng thái: %s). " +
                                    "Vui lòng hoàn tất hoặc hủy trước khi tạo đặt chỗ mới.",
                            existingBooking.getId(),
                            existingBooking.getStatus())
            );
        }

        // Validate station
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));

        // Validate station có cùng loại pin với xe
        if (!station.getBatteryType().getId().equals(vehicle.getBatteryType().getId())) {
            throw new AuthenticationException("Trạm không hỗ trợ loại pin của xe bạn");
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
            throw new NotFoundException(
                    "Khong co pin nao du dien (>= 95%) tai tram nay. " +
                            "Vui long chon tram khac hoac doi sau."
            );
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
        Booking booking = bookingRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đặt chỗ"));

        LocalDateTime now = LocalDateTime.now();

        // TÍNH THỜI GIAN CÒN LẠI ĐẾN GIỜ BOOKING
        long minutesUntilBooking = Duration.between(now, booking.getBookingTime()).toMinutes();

        // QUAN TRỌNG: CHỈ CHO PHÉP HỦY KHI CÒN TRÊN 1 TIẾNG
        if (minutesUntilBooking <= ALLOW_CANCEL_BEFORE_MINUTES) {
            throw new AuthenticationException(
                    String.format(
                            "Không thể hủy booking. Chỉ được hủy trước giờ đặt lịch %d phút. " +
                                    "Thời gian còn lại: %d phút. " +
                                    "Vui long liên hệ staff để được hỗ trợ.",
                            ALLOW_CANCEL_BEFORE_MINUTES,
                            minutesUntilBooking
                    )
            );
        }

        // Kiểm tra nếu booking đã COMPLETED hoặc CANCELLED
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new AuthenticationException("Không thể hủy booking đã hoàn thành");
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new AuthenticationException("Booking này đã được hủy trước đó");
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

        // Hủy booking
        booking.setStatus(Booking.Status.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);

        // Gửi email thông báo hủy booking
        sendBookingCancellationEmail(savedBooking);

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
            throw new AuthenticationException("Chi Staff/Admin moi duoc huy booking");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay booking voi ID: " + id));

        // STAFF CHỈ CANCEL ĐƯỢC BOOKING CỦA TRẠM MÌNH QUẢN LÝ
        if (currentUser.getRole() == User.Role.STAFF) {
            Station bookingStation = booking.getStation();
            if (bookingStation == null) {
                throw new AuthenticationException("lịch đặt không có trạm liên quan");
            }

            if (!staffStationAssignmentRepository.existsByStaffAndStation(currentUser, bookingStation)) {
                throw new AuthenticationException("Bạn không được phân công quản lý trạm này. Chỉ có thể hủy đặt chỗ của những trạm do bạn quản lý.\n");
            }
        }

        // Kiểm tra: Không cho hủy booking đã COMPLETED hoặc CANCELLED
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new AuthenticationException("Không thể hủy đặt chỗ đã hoàn tất");
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new AuthenticationException("Không thể hủy đặt chỗ đã hủy trước đó");
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

        // Hủy booking
        booking.setStatus(Booking.Status.CANCELLED);

        System.out.println(String.format(
                "Nhân viên đã hủy đơn đặt chỗ. Mã đơn: %d, Mã tài xế: %d, Mã nhân viên: %d, Lý do: %s",
                booking.getId(), booking.getDriver().getId(), currentUser.getId(),
                reason != null ? reason : "Không có lí do"
        ));

        // Gửi email thông báo hủy booking
        sendBookingCancellationEmail(booking);

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
            emailDetail.setVehiclePlateNumber(vehicle.getPlateNumber()); // ✅ Biển số riêng
            
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
    private void sendBookingCancellationEmail(Booking booking) {
        try {
            User driver = booking.getDriver();
            Vehicle vehicle = booking.getVehicle();
            Station station = booking.getStation();

            EmailDetail emailDetail = new EmailDetail();
            emailDetail.setRecipient(driver.getEmail());
            emailDetail.setSubject("THÔNG BÁO HỦY ĐẶT LỊCH - " + booking.getConfirmationCode());
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
            emailDetail.setVehiclePlateNumber(vehicle.getPlateNumber()); // ✅ Biển số riêng
            
            emailDetail.setBatteryType(
                    station.getBatteryType().getName() +
                            (station.getBatteryType().getCapacity() != null ? " - " + station.getBatteryType().getCapacity() + "kWh" : "")
            );
            emailDetail.setStatus("HỦY");
            // Mã xác nhận KHÔNG kèm biển số
            emailDetail.setConfirmationCode(booking.getConfirmationCode());

            // Thêm thông tin chính sách hủy
            emailDetail.setCancellationPolicy("Lịch đặt của bạn đã được hủy thành công. Pin đã được giải phóng.");

            emailService.sendBookingCancellationEmail(emailDetail);
        } catch (Exception e) {
            System.err.println("Gửi email xác nhận đơn đặt chỗ thất bại: " + e.getMessage());
        }
    }

    // ==================== CÁC METHOD KHÁC ====================

    @Transactional(readOnly = true)
    public List<Booking> getMyBookings() {
        User currentUser = authenticationService.getCurrentUser();
        return bookingRepository.findByDriver(currentUser);
    }

    @Transactional(readOnly = true)
    public Booking getMyBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        return bookingRepository.findByIdAndDriver(id, currentUser)
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
            throw new AuthenticationException("Từ chối truy cập");
        }

        if (currentUser.getRole() == User.Role.ADMIN) {
            return bookingRepository.findAll();
        }

        List<Station> myStations = staffStationAssignmentRepository.findStationsByStaff(currentUser);
        if (myStations.isEmpty()) {
            return List.of();
        }

        return bookingRepository.findByStationIn(myStations);
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsForMyStations() {
        User currentUser = authenticationService.getCurrentUser();

        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Chỉ nhân viên hoặc quản trị viên mới có quyền xem danh sách đơn đặt chỗ.");
        }

        if (currentUser.getRole() == User.Role.ADMIN) {
            return bookingRepository.findAll();
        }

        List<Station> myStations = staffStationAssignmentRepository.findStationsByStaff(currentUser);

        if (myStations.isEmpty()) {
            throw new AuthenticationException("Bạn chưa được phân công vào trạm nào.");
        }

        return bookingRepository.findByStationIn(myStations);
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Từ chối truy cập");
        }
        return bookingRepository.findByStationId(stationId);
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStatus(Booking.Status status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Từ chối truy cập");
        }
        return bookingRepository.findByStatus(status);
    }



    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}