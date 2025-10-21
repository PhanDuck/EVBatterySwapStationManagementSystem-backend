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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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

    /**
     * CREATE - Tao booking moi (Driver)
     * 
     * BAT BUOC PHAI CO SUBSCRIPTION:
     * - Driver phai co subscription ACTIVE
     * - RemainingSwaps > 0 (con luot swap)
     * - StartDate <= Today <= EndDate
     * 
     * CHI TRU LUOT SWAP KHI:
     * - Booking CONFIRMED ma driver KHONG DEN (sau 3 tieng)
     * - Swap thanh cong (COMPLETED)
     * 
     * KHONG TRU LUOT SWAP KHI:
     * - Booking van PENDING (chua confirm)
     * - Driver tu huy PENDING (huy som)
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

        // VALIDATION 1: Kiem tra con luot swap khong (QUAN TRONG!)
        if (activeSubscription.getRemainingSwaps() <= 0) {
            throw new AuthenticationException(
                    "Goi dich vu cua ban da het luot swap. " +
                    "Vui long gia han hoac mua goi moi."
            );
        }

        //Kiểm tra driver đã có booking đang hoạt động chưa
        List<Booking> activeBookings = bookingRepository.findByDriverAndStatusNotIn(
                currentUser,
                List.of(Booking.Status.CANCELLED, Booking.Status.COMPLETED)
        );

        if (!activeBookings.isEmpty()) {
            throw new AuthenticationException("You already have an active booking. Please Complete or Cancel it before creating a new one.");
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

        // Tạo booking thủ công thay vì dùng ModelMapper để tránh conflict
        Booking booking = new Booking();
        booking.setDriver(currentUser);
        booking.setVehicle(vehicle);
        booking.setStation(station);
        booking.setBookingTime(request.getBookingTime());
        
        // No confirmation code when creating booking
        // Code will be generated when Staff/Admin confirms booking
        booking.setConfirmationCode(null);
        booking.setStatus(Booking.Status.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        // Gửi email xác nhận booking
        sendBookingConfirmationEmail(savedBooking, currentUser, vehicle, station);

        return savedBooking;
    }

    /**
     * Gửi email xác nhận đặt lịch
     */
    private void sendBookingConfirmationEmail(Booking booking, User driver, Vehicle vehicle, Station station) {
        try {
            //Tạo email detail
            EmailDetail emailDetail = new EmailDetail();
            emailDetail.setRecipient(driver.getEmail());
            emailDetail.setSubject("Xác nhận đặt lịch thay pin - EV Battery Swap");
            emailDetail.setFullName(driver.getFullName());

            // Thông tin booking
            emailDetail.setBookingId(booking.getId());
            emailDetail.setStationName(station.getName());
            emailDetail.setStationLocation(
                    station.getLocation() != null ? station.getLocation() :
                            (station.getDistrict() + ", " + station.getCity())
            );
            emailDetail.setStationContact(station.getContactInfo() != null ? station.getContactInfo() : "Chưa cập nhật");

            // Format thời gian
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            emailDetail.setBookingTime(booking.getBookingTime().format(formatter));

            emailDetail.setVehicleModel(vehicle.getModel() != null ? vehicle.getModel() : vehicle.getPlateNumber());
            emailDetail.setBatteryType(
                    station.getBatteryType().getName() +
                            (station.getBatteryType().getCapacity() != null ? " - " + station.getBatteryType().getCapacity() + "kWh" : "")
            );
            emailDetail.setStatus(booking.getStatus().toString());

            // Gửi email bất đồng bộ (không chặn luồng chính)
            emailService.sendBookingConfirmationEmail(emailDetail);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không ảnh hưởng đến booking
            System.err.println("Failed to send booking confirmation email: " + e.getMessage());
        }
    }

    /**
     * READ - Lấy bookings của driver hiện tại
     */
    @Transactional(readOnly = true)
    public List<Booking> getMyBookings() {
        User currentUser = authenticationService.getCurrentUser();
        return bookingRepository.findByDriver(currentUser);
    }

    /**
     * READ - Lấy booking cụ thể của driver
     */
    @Transactional(readOnly = true)
    public Booking getMyBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        return bookingRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    /**
     * READ - Lấy stations tương thích với vehicle (Public)
     */
    @Transactional(readOnly = true)
    public List<Station> getCompatibleStations(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));

        // Chi lay stations co cung battery type, ACTIVE, VA co pin >80% san sang
        return stationRepository.findStationsWithAvailableBatteries(
                vehicle.getBatteryType(),
                80  // Yeu cau pin tren 80% suc khoe
        );
    }

    // ... (các method khác giữ nguyên)

    /**
     * UPDATE - Huy booking (Driver)
     * 
     * CHI CHO PHEP HUY KHI PENDING (chua confirm)
     * - Sau khi Staff confirm → KHONG CHO HUY
     * - Ly do: Pin da duoc giu (PENDING), neu khong den se bi tru luot
     * - Neu muon huy sau khi confirm → Lien he staff
     */
    @Transactional
    public Booking cancelMyBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        Booking booking = bookingRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        // QUAN TRONG: CHI CHO PHEP HUY KHI PENDING
        if (booking.getStatus() != Booking.Status.PENDING) {
            String message = String.format(
                "Khong the huy booking voi trang thai '%s'. " +
                "Chi co the huy booking PENDING (chua confirm). " +
                "Neu da CONFIRMED, vui long lien he staff hoac den tram dung gio.",
                booking.getStatus()
            );
            throw new AuthenticationException(message);
        }

        // Huy booking (PENDING → CANCELLED)
        booking.setStatus(Booking.Status.CANCELLED);
        return bookingRepository.save(booking);
    }

    /**
     * UPDATE - Huy booking boi Staff/Admin (truong hop dac biet)
     * 
     * Staff co the huy bat ky booking nao (PENDING hoac CONFIRMED)
     * Ly do: Tram bao tri, pin hong, khan cap, etc.
     * 
     * Neu huy CONFIRMED booking:
     * - Giai phong pin (PENDING → AVAILABLE)
     * - KHONG TRU luot swap (loi tu phia tram, khong phai loi driver)
     * - Clear reservation fields

     */
    @Transactional
    public Booking cancelBookingByStaff(Long id, String reason) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Chi Staff/Admin moi duoc huy booking");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay booking voi ID: " + id));

        // Kiem tra: Khong cho huy booking da COMPLETED hoac da CANCELLED
        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new AuthenticationException("Khong the huy booking da COMPLETED");
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new AuthenticationException("Booking nay da duoc huy truoc do");
        }

        // NEU BOOKING DA CONFIRMED VA CO PIN RESERVED → GIAI PHONG PIN
        if (booking.getStatus() == Booking.Status.CONFIRMED && booking.getReservedBattery() != null) {
            Battery battery = booking.getReservedBattery();
            
            // Giai phong pin (PENDING → AVAILABLE)
            if (battery.getStatus() == Battery.Status.PENDING) {
                battery.setStatus(Battery.Status.AVAILABLE);
                battery.setReservedForBooking(null);
                battery.setReservationExpiry(null);
                batteryRepository.save(battery);
                
                // Log de tracking
                System.out.println(String.format(
                    "Staff huy booking CONFIRMED. BookingID: %d, StaffID: %d, Reason: %s, BatteryID: %d da giai phong",
                    booking.getId(), currentUser.getId(), 
                    reason != null ? reason : "Khong co ly do", 
                    battery.getId()
                ));
            }
            
            // Clear booking reservation
            booking.setReservedBattery(null);
            booking.setReservationExpiry(null);
        }

        // Huy booking
        booking.setStatus(Booking.Status.CANCELLED);
        
        // Log de tracking
        System.out.println(String.format(
            "Staff huy booking. BookingID: %d, DriverID: %d, StaffID: %d, Reason: %s",
            booking.getId(), booking.getDriver().getId(), currentUser.getId(), 
            reason != null ? reason : "Khong co ly do"
        ));
        
        return bookingRepository.save(booking);
    }

    /**
     * READ - Lấy tất cả bookings (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return bookingRepository.findAll();
    }

    /**
     * READ - Lay bookings cua cac tram Staff quan ly (Staff only)
     * 
     * Staff chi xem duoc bookings cua cac tram minh duoc assign
     * Admin xem duoc tat ca bookings
     * 
     * Dung de hien thi danh sach booking can confirm
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsForMyStations() {
        User currentUser = authenticationService.getCurrentUser();
        
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Chi Staff/Admin moi xem duoc bookings");
        }

        // Admin xem tat ca
        if (currentUser.getRole() == User.Role.ADMIN) {
            return bookingRepository.findAll();
        }

        // Staff chi xem booking cua cac tram minh quan ly
        List<Station> myStations = staffStationAssignmentRepository.findStationsByStaff(currentUser);
        
        if (myStations.isEmpty()) {
            throw new AuthenticationException("Ban chua duoc assign vao tram nao");
        }

        // Lay tat ca bookings cua cac tram minh quan ly
        return bookingRepository.findByStationIn(myStations);
    }


    /**
     * READ - Lấy bookings theo station (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return bookingRepository.findByStationId(stationId);
    }

    /**
     * READ - Lấy bookings theo status (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStatus(Booking.Status status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return bookingRepository.findByStatus(status);
    }

    /**
     * CONFIRM BOOKING BY ID (Staff/Admin only)
     * 
     * Khi Staff/Admin confirm booking:
     * 1. Generate ma xac nhan 6 ky tu (ABC123)
     *
     * 3. RESERVE PIN CHO DRIVER (status = PENDING)
     * 4. DAT THOI HAN 3 TIENG (auto-cancel neu khong swap)
     * 5. Chuyen status: PENDING → CONFIRMED
     * 6. Tra code cho driver
     * 
     * Driver se dung code nay de tu swap pin tai tram
     */
    @Transactional
    public Booking confirmBookingById(Long bookingId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Chi Staff/Admin moi duoc confirm booking");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay booking voi ID: " + bookingId));

        if (booking.getStatus() != Booking.Status.PENDING) {
            throw new AuthenticationException(
                    "Chi confirm duoc booking PENDING. " +
                    "Booking nay dang o trang thai: " + booking.getStatus()
            );
        }

        // BUOC 1: TIM PIN AVAILABLE, , SUCK KHOE >= 70% TAI TRAM, DUNG LOAI PIN
        BatteryType requiredBatteryType = booking.getVehicle().getBatteryType();
        
        List<Battery> availableBatteries = batteryRepository.findAll()
                .stream()
                .filter(b -> b.getCurrentStation() != null 
                        && b.getCurrentStation().getId().equals(booking.getStation().getId())
                        && b.getBatteryType().getId().equals(requiredBatteryType.getId())
                        && b.getStatus() == Battery.Status.AVAILABLE
                        && b.getChargeLevel().compareTo(BigDecimal.valueOf(95)) >= 0
                        && b.getStateOfHealth() != null 
                        && b.getStateOfHealth().compareTo(BigDecimal.valueOf(70)) >= 0)  //Health >= 70%
                .sorted((b1, b2) -> {
                    // Ưu tiên: Sức khỏe cao nhất → Điện cao nhất
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

        // BUOC 2: LAY PIN CO SUC KHOE CAO NHAT
        Battery reservedBattery = availableBatteries.get(0);

        // BUOC 3: RESERVE PIN (status = PENDING, khoa trong 3 tieng)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = now.plusHours(3);

        reservedBattery.setStatus(Battery.Status.PENDING);
        reservedBattery.setReservedForBooking(booking);
        reservedBattery.setReservationExpiry(expiryTime);
        batteryRepository.save(reservedBattery);

        // BUOC 4: CAP NHAT BOOKING
        booking.setReservedBattery(reservedBattery);
        booking.setReservationExpiry(expiryTime);

        // BUOC 5: GENERATE CODE KHI CONFIRM (khong phai khi tao booking)
        String confirmationCode = com.evbs.BackEndEvBs.util.ConfirmationCodeGenerator.generateUnique(
            10, // Thu toi da 10 lan
            code -> bookingRepository.findByConfirmationCode(code).isPresent()
        );
        booking.setConfirmationCode(confirmationCode);
        booking.setStatus(Booking.Status.CONFIRMED);
        booking.setConfirmedBy(currentUser);  // LUU NGUOI CONFIRM

        Booking savedBooking = bookingRepository.save(booking);

        //  GỬI EMAIL CONFIRMATION VỚI CODE
        sendBookingConfirmedEmail(savedBooking, currentUser);

        return savedBooking;
    }


    /**
     * Gửi email xác nhận booking đã được approve với confirmation code
     */
    private void sendBookingConfirmedEmail(Booking booking, User confirmedBy) {
        try {
            User driver = booking.getDriver();
            Vehicle vehicle = booking.getVehicle();
            Station station = booking.getStation();

            // Tạo email detail
            EmailDetail emailDetail = new EmailDetail();
            emailDetail.setRecipient(driver.getEmail());
            emailDetail.setSubject("Booking được xác nhận - Mã swap pin: " + booking.getConfirmationCode());
            emailDetail.setFullName(driver.getFullName());

            // Thông tin booking
            emailDetail.setBookingId(booking.getId());
            emailDetail.setStationName(station.getName());
            emailDetail.setStationLocation(
                    station.getLocation() != null ? station.getLocation() :
                            (station.getDistrict() + ", " + station.getCity())
            );
            emailDetail.setStationContact(station.getContactInfo() != null ? station.getContactInfo() : "Chưa cập nhật");

            // Format thời gian
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            emailDetail.setBookingTime(booking.getBookingTime().format(formatter));

            emailDetail.setVehicleModel(vehicle.getModel() != null ? vehicle.getModel() : vehicle.getPlateNumber());
            emailDetail.setBatteryType(
                    station.getBatteryType().getName() +
                            (station.getBatteryType().getCapacity() != null ? " - " + station.getBatteryType().getCapacity() + "kWh" : "")
            );
            emailDetail.setStatus(booking.getStatus().toString());

            //  THÊM CONFIRMATION CODE VÀO EMAIL
            emailDetail.setConfirmationCode(booking.getConfirmationCode());
            emailDetail.setConfirmedBy(confirmedBy.getFullName());

            // Gửi email bất đồng bộ
            emailService.sendBookingConfirmedEmail(emailDetail);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không ảnh hưởng đến booking
            System.err.println("Failed to send booking confirmed email: " + e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}