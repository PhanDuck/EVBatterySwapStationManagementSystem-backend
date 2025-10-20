package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.EmailDetail;
import com.evbs.BackEndEvBs.model.request.BookingRequest;
import com.evbs.BackEndEvBs.repository.BookingRepository;
import com.evbs.BackEndEvBs.repository.DriverSubscriptionRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
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
    private final AuthenticationService authenticationService;

    @Autowired
    private EmailService emailService;

    /**
     * CREATE - Tạo booking mới (Driver)
     * 
     * ✅ BẮT BUỘC PHẢI CÓ SUBSCRIPTION:
     * - Driver phải có subscription ACTIVE
     * - RemainingSwaps > 0
     * - StartDate <= Today <= EndDate
     */
    @Transactional
    public Booking createBooking(BookingRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        // ✅ VALIDATION 0: BẮT BUỘC phải có subscription ACTIVE
        DriverSubscription activeSubscription = driverSubscriptionRepository
                .findActiveSubscriptionByDriver(currentUser, LocalDate.now())
                .orElseThrow(() -> new AuthenticationException(
                        "❌ BẮT BUỘC: Bạn phải mua gói dịch vụ trước khi booking. " +
                        "Vui lòng mua ServicePackage và tạo DriverSubscription trước."
                ));

        // ✅ VALIDATION 1: Kiểm tra còn lượt swap không
        if (activeSubscription.getRemainingSwaps() <= 0) {
            throw new AuthenticationException(
                    "❌ Gói dịch vụ của bạn đã hết lượt swap. " +
                    "Vui lòng gia hạn hoặc mua gói mới."
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
        
        // ⭐ THAY ĐỔI: KHÔNG generate code khi tạo booking
        // Code sẽ được generate khi Staff/Admin CONFIRM booking
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

        // Lấy tất cả stations có cùng battery type với vehicle và status ACTIVE
        return stationRepository.findByBatteryTypeAndStatus(
                vehicle.getBatteryType(),
                Station.Status.ACTIVE
        );
    }

    // ... (các method khác giữ nguyên)

    /**
     * UPDATE - Hủy booking (Driver)
     */
    @Transactional
    public Booking cancelMyBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        Booking booking = bookingRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        // Kiểm tra trạng thái booking - chỉ cho phép hủy khi status là PENDING
        if (booking.getStatus() != Booking.Status.PENDING) {
            String message = String.format("Cannot cancel booking with status '%s'. Only PENDING bookings can be cancelled by driver.",
                    booking.getStatus());
            throw new AuthenticationException(message);
        }

        booking.setStatus(Booking.Status.CANCELLED);
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
     * UPDATE - Cập nhật booking status (Admin/Staff only)
     */
    @Transactional
    public Booking updateBookingStatus(Long id, Booking.Status newStatus) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        Booking.Status currentStatus = booking.getStatus();

        // 🔹 Không cho đổi sang cùng trạng thái
        if (currentStatus == newStatus) {
            throw new AuthenticationException("Booking already has status: " + newStatus);
        }

        // 🔹 Kiểm tra logic chuyển trạng thái hợp lệ
        switch (currentStatus) {
            case PENDING -> {
                if (newStatus != Booking.Status.CONFIRMED && newStatus != Booking.Status.CANCELLED) {
                    throw new AuthenticationException("Cannot change from PENDING to " + newStatus);
                }
            }
            case CONFIRMED -> {
                if (newStatus != Booking.Status.COMPLETED && newStatus != Booking.Status.CANCELLED) {
                    throw new AuthenticationException("Cannot change from CONFIRMED to " + newStatus);
                }
            }
            case COMPLETED, CANCELLED -> {
                throw new AuthenticationException("Cannot change status of a finished booking.");
            }
        }

        // 🔹 Nếu hợp lệ, cập nhật
        booking.setStatus(newStatus);
        return bookingRepository.save(booking);
    }

    /**
     * DELETE - Xóa booking (Admin only)
     */
    @Transactional
    public void deleteBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        bookingRepository.delete(booking);
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
     * ✅ CONFIRM BOOKING BY ID (Staff/Admin only)
     * 
     * Khi Staff/Admin confirm booking:
     * 1. Generate mã xác nhận 6 ký tự (ABC123)
     * 2. Chuyển status: PENDING → CONFIRMED
     * 3. Trả code cho driver
     * 
     * Driver sẽ dùng code này để tự swap pin tại trạm
     */
    @Transactional
    public Booking confirmBookingById(Long bookingId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("❌ Chỉ Staff/Admin mới được confirm booking");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("❌ Không tìm thấy booking với ID: " + bookingId));

        if (booking.getStatus() != Booking.Status.PENDING) {
            throw new AuthenticationException(
                    "❌ Chỉ confirm được booking PENDING. " +
                    "Booking này đang ở trạng thái: " + booking.getStatus()
            );
        }

        // ⭐ GENERATE CODE KHI CONFIRM (không phải khi tạo booking)
        String confirmationCode = com.evbs.BackEndEvBs.util.ConfirmationCodeGenerator.generateUnique(
            10, // Thử tối đa 10 lần
            code -> bookingRepository.findByConfirmationCode(code).isPresent()
        );
        booking.setConfirmationCode(confirmationCode);
        booking.setStatus(Booking.Status.CONFIRMED);
        booking.setConfirmedBy(currentUser);  // ⭐ LƯU NGƯỜI CONFIRM
        
        return bookingRepository.save(booking);
    }

    /**
     * ✅ VERIFY BOOKING BY CONFIRMATION CODE (Staff only)
     * 
     * Staff nhập confirmation code (ABC123) từ driver
     * → Trả về thông tin booking để xác nhận
     * → Staff có thể confirm booking (PENDING → CONFIRMED)
     */
    @Transactional(readOnly = true)
    public Booking verifyBookingByCode(String confirmationCode) {
        User currentStaff = authenticationService.getCurrentUser();
        if (currentStaff.getRole() != User.Role.STAFF) {
            throw new AuthenticationException("❌ Chỉ Staff mới được verify booking");
        }

        Booking booking = bookingRepository.findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new NotFoundException(
                        "❌ Không tìm thấy booking với mã: " + confirmationCode
                ));

        // Kiểm tra status - chỉ cho verify booking PENDING hoặc CONFIRMED
        if (booking.getStatus() == Booking.Status.COMPLETED || 
            booking.getStatus() == Booking.Status.CANCELLED) {
            throw new AuthenticationException(
                    "❌ Booking này đã " + booking.getStatus() + 
                    " (không thể swap nữa)"
            );
        }

        return booking;
    }

    /**
     * ✅ CONFIRM BOOKING BY CODE (Staff only) - DEPRECATED
     * 
     * Sau khi verify, staff confirm booking
     * PENDING → CONFIRMED
     * 
     * CHƯA TRỪ remainingSwaps (chỉ trừ khi swap hoàn tất)
     * 
     * @deprecated Use confirmBookingById(Long bookingId) instead
     */
    @Deprecated
    @Transactional
    public Booking confirmBookingByCode(String confirmationCode) {
        User currentStaff = authenticationService.getCurrentUser();
        if (currentStaff.getRole() != User.Role.STAFF) {
            throw new AuthenticationException("❌ Chỉ Staff mới được confirm booking");
        }

        Booking booking = bookingRepository.findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new NotFoundException(
                        "❌ Không tìm thấy booking với mã: " + confirmationCode
                ));

        if (booking.getStatus() != Booking.Status.PENDING) {
            throw new AuthenticationException(
                    "❌ Chỉ confirm được booking PENDING. " +
                    "Booking này đang ở trạng thái: " + booking.getStatus()
            );
        }

        booking.setStatus(Booking.Status.CONFIRMED);
        return bookingRepository.save(booking);
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}