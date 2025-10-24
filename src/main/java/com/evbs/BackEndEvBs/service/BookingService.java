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

    // Cấu hình thời gian cho phép hủy booking (phút)
    private static final int ALLOW_CANCEL_BEFORE_MINUTES = 30;

    /**
     * CREATE - Tao booking moi (Driver) - TỰ ĐỘNG CONFIRM VÀ GỬI MÃ NGAY
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

        // Kiểm tra driver đã có booking đang hoạt động chưa
        List<Booking> activeBookings = bookingRepository.findByDriverAndStatusNotIn(
                currentUser,
                List.of(Booking.Status.CANCELLED, Booking.Status.COMPLETED)
        );

        if (!activeBookings.isEmpty()) {
            throw new AuthenticationException("You already have an active booking. Please Complete or Cancel it before creating a new one.");
        }

        // VALIDATION: Max 10 bookings per user per day
        LocalDate today = LocalDate.now();
        long bookingsToday = bookingRepository.findByDriver(currentUser)
                .stream()
                .filter(b -> b.getBookingTime() != null && b.getBookingTime().toLocalDate().isEqual(today))
                .count();
        if (bookingsToday >= 10) {
            throw new AuthenticationException("You have reached the maximum of 10 bookings for today.");
        }

        // Validate vehicle thuộc về driver
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));

        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Vehicle does not belong to current user");
        }

        // Validate station
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Station not found"));

        // Validate station có cùng loại pin với xe
        if (!station.getBatteryType().getId().equals(vehicle.getBatteryType().getId())) {
            throw new AuthenticationException("Station does not support the battery type of your vehicle");
        }

        // VALIDATION: Chỉ cho phép đặt lịch trong vòng 3 tiếng tới
        if (request.getBookingTime() == null) {
            throw new AuthenticationException("Booking time is required");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxBookingTime = now.plusHours(3);

        // Không được đặt lịch quá khứ
        if (request.getBookingTime().isBefore(now)) {
            throw new AuthenticationException("Không thể đặt lịch trong quá khứ");
        }

        // Chỉ được đặt trong vòng 3 tiếng tới
        if (request.getBookingTime().isAfter(maxBookingTime)) {
            throw new AuthenticationException(
                    "Chỉ được đặt lịch trong vòng 3 tiếng tới. " +
                            "Thời gian muộn nhất: " +
                            maxBookingTime.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
            );
        }

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
        LocalDateTime expiryTime = now.plusHours(3);

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
        booking.setBookingTime(request.getBookingTime());
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
     * Hủy booking (Driver) - CHỈ CHO HỦY TRƯỚC 30 PHÚT
     */
    @Transactional
    public Booking cancelMyBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        Booking booking = bookingRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        LocalDateTime now = LocalDateTime.now();

        // TÍNH THỜI GIAN CÒN LẠI ĐẾN GIỜ BOOKING
        long minutesUntilBooking = Duration.between(now, booking.getBookingTime()).toMinutes();

        // QUAN TRỌNG: CHỈ CHO PHÉP HỦY KHI CÒN TRÊN 30 PHÚT
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
                throw new AuthenticationException("Booking khong co station");
            }

            if (!staffStationAssignmentRepository.existsByStaffAndStation(currentUser, bookingStation)) {
                throw new AuthenticationException("Ban khong duoc phan cong quan ly tram nay. Chi co the huy booking cua tram minh quan ly.");
            }
        }

        // Kiểm tra: Không cho hủy booking đã COMPLETED hoặc CANCELLED
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new AuthenticationException("Khong the huy booking da COMPLETED");
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new AuthenticationException("Booking nay da duoc huy truoc do");
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
                        "Staff hủy booking CONFIRMED. BookingID: %d, StaffID: %d, Reason: %s, BatteryID: %d da giai phong",
                        booking.getId(), currentUser.getId(),
                        reason != null ? reason : "Khong co ly do",
                        battery.getId()
                ));
            }

            booking.setReservedBattery(null);
            booking.setReservationExpiry(null);
        }

        // Hủy booking
        booking.setStatus(Booking.Status.CANCELLED);

        System.out.println(String.format(
                "Staff hủy booking. BookingID: %d, DriverID: %d, StaffID: %d, Reason: %s",
                booking.getId(), booking.getDriver().getId(), currentUser.getId(),
                reason != null ? reason : "Khong co ly do"
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
            emailDetail.setSubject("ĐẶT LỊCH THÀNH CÔNG - Mã swap pin: " + booking.getConfirmationCode());
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

            emailDetail.setVehicleModel(vehicle.getModel() != null ? vehicle.getModel() : vehicle.getPlateNumber());
            emailDetail.setBatteryType(
                    station.getBatteryType().getName() +
                            (station.getBatteryType().getCapacity() != null ? " - " + station.getBatteryType().getCapacity() + "kWh" : "")
            );
            emailDetail.setStatus(booking.getStatus().toString());

            emailDetail.setConfirmationCode(booking.getConfirmationCode());
            emailDetail.setConfirmedBy(confirmedBy != null ? confirmedBy.getFullName() : "Hệ thống");

            // Thêm thông tin về chính sách hủy booking
            emailDetail.setCancellationPolicy(
                    String.format("Lưu ý: Bạn chỉ có thể hủy booking trước %d phút so với giờ đã đặt.", ALLOW_CANCEL_BEFORE_MINUTES)
            );

            emailService.sendBookingConfirmedEmail(emailDetail);
        } catch (Exception e) {
            System.err.println("Failed to send booking confirmed email: " + e.getMessage());
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
            emailDetail.setSubject("THÔNG BÁO HỦY BOOKING - " + booking.getConfirmationCode());
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

            emailDetail.setVehicleModel(vehicle.getModel() != null ? vehicle.getModel() : vehicle.getPlateNumber());
            emailDetail.setBatteryType(
                    station.getBatteryType().getName() +
                            (station.getBatteryType().getCapacity() != null ? " - " + station.getBatteryType().getCapacity() + "kWh" : "")
            );
            emailDetail.setStatus("CANCELLED");
            emailDetail.setConfirmationCode(booking.getConfirmationCode());

            // Thêm thông tin chính sách hủy
            emailDetail.setCancellationPolicy("Booking của bạn đã được hủy thành công. Pin đã được giải phóng.");

            emailService.sendBookingCancellationEmail(emailDetail);
        } catch (Exception e) {
            System.err.println("Failed to send booking cancellation email: " + e.getMessage());
        }
    }

    // ==================== CÁC METHOD KHÁC GIỮ NGUYÊN ====================

    @Transactional(readOnly = true)
    public List<Booking> getMyBookings() {
        User currentUser = authenticationService.getCurrentUser();
        return bookingRepository.findByDriver(currentUser);
    }

    @Transactional(readOnly = true)
    public Booking getMyBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        return bookingRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    @Transactional(readOnly = true)
    public List<Station> getCompatibleStations(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));
        return stationRepository.findStationsWithAvailableBatteries(
                vehicle.getBatteryType(),
                80
        );
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
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
            throw new AuthenticationException("Chi Staff/Admin moi xem duoc bookings");
        }

        if (currentUser.getRole() == User.Role.ADMIN) {
            return bookingRepository.findAll();
        }

        List<Station> myStations = staffStationAssignmentRepository.findStationsByStaff(currentUser);

        if (myStations.isEmpty()) {
            throw new AuthenticationException("Ban chua duoc assign vao tram nao");
        }

        return bookingRepository.findByStationIn(myStations);
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return bookingRepository.findByStationId(stationId);
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStatus(Booking.Status status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return bookingRepository.findByStatus(status);
    }

    @Transactional
    public Booking autoConfirmBooking(Booking booking) {
        if (booking == null) return null;
        if (booking.getStatus() != Booking.Status.PENDING) return booking;

        BatteryType requiredBatteryType = booking.getVehicle().getBatteryType();

        List<Battery> availableBatteries = batteryRepository.findAll()
                .stream()
                .filter(b -> b.getCurrentStation() != null
                        && b.getCurrentStation().getId().equals(booking.getStation().getId())
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
            try {
                EmailDetail detail = new EmailDetail();
                detail.setRecipient(booking.getDriver().getEmail());
                detail.setSubject("Thông báo: Trạm không còn pin phù hợp cho booking");
                detail.setFullName(booking.getDriver().getFullName());
                detail.setBookingId(booking.getId());
                detail.setStationName(booking.getStation().getName());
                detail.setStationLocation(booking.getStation().getLocation() != null ? booking.getStation().getLocation() : (booking.getStation().getDistrict() + ", " + booking.getStation().getCity()));
                detail.setStationContact(booking.getStation().getContactInfo() != null ? booking.getStation().getContactInfo() : "Chưa cập nhật");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
                detail.setBookingTime(booking.getBookingTime() != null ? booking.getBookingTime().format(formatter) : "N/A");
                detail.setVehicleModel(booking.getVehicle() != null ? (booking.getVehicle().getModel() != null ? booking.getVehicle().getModel() : booking.getVehicle().getPlateNumber()) : "N/A");
                detail.setBatteryType(booking.getStation().getBatteryType().getName());
                detail.setStatus("OUT_OF_STOCK");
                emailService.sendBookingConfirmationEmail(detail);
            } catch (Exception ignore) {}
            return booking;
        }

        Battery reservedBattery = availableBatteries.get(0);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = now.plusHours(3);

        reservedBattery.setStatus(Battery.Status.PENDING);
        reservedBattery.setReservedForBooking(booking);
        reservedBattery.setReservationExpiry(expiryTime);
        batteryRepository.save(reservedBattery);

        booking.setReservedBattery(reservedBattery);
        booking.setReservationExpiry(expiryTime);

        String confirmationCode = com.evbs.BackEndEvBs.util.ConfirmationCodeGenerator.generateUnique(
                10, code -> bookingRepository.findByConfirmationCode(code).isPresent()
        );
        booking.setConfirmationCode(confirmationCode);
        booking.setStatus(Booking.Status.CONFIRMED);

        try {
            userRepository.findAll()
                    .stream()
                    .filter(u -> u.getRole() == User.Role.ADMIN)
                    .findFirst()
                    .ifPresent(booking::setConfirmedBy);
        } catch (Exception ignore) {}

        Booking saved = bookingRepository.save(booking);
        sendBookingConfirmedEmail(saved, saved.getConfirmedBy() != null ? saved.getConfirmedBy() : null);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Booking> findBookingsByPhone(String phoneNumber) {
        return userRepository.findAll()
                .stream()
                .filter(u -> phoneNumber != null && phoneNumber.equals(u.getPhoneNumber()))
                .findFirst()
                .map(bookingRepository::findByDriver)
                .orElse(List.of());
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}