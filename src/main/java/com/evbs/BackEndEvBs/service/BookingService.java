package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.BookingRequest;
import com.evbs.BackEndEvBs.repository.BookingRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final AuthenticationService authenticationService;

    // ==================== FULL CRUD (Admin/Staff) ====================

    /**
     * CREATE - Tạo booking mới (Driver/Admin/Staff)
     */
    @Transactional
    public Booking createBooking(BookingRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));

        // Driver chỉ được tạo booking cho xe của chính mình
        if (currentUser.getRole() == User.Role.DRIVER && !vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("You can only create bookings for your own vehicles");
        }

        // Kiểm tra thời gian booking (không được trong quá khứ)
        if (request.getBookingTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking time cannot be in the past");
        }

        Booking booking = new Booking();
        booking.setDriver(currentUser.getRole() == User.Role.DRIVER ? currentUser : vehicle.getDriver());
        booking.setVehicle(vehicle);
        booking.setStation(station);
        booking.setBookingTime(request.getBookingTime());
        booking.setStatus("Pending");

        return bookingRepository.save(booking);
    }

    /**
     * READ - Lấy tất cả bookings (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return bookingRepository.findAll();
    }

    /**
     * READ - Lấy booking theo ID (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + id));
    }

    /**
     * READ - Lấy bookings theo station (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + stationId));

        return bookingRepository.findByStationOrderByBookingTimeDesc(station);
    }

    /**
     * READ - Lấy bookings theo status (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByStatus(String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return bookingRepository.findByStatusOrderByBookingTimeDesc(status);
    }

    /**
     * DELETE - Xóa booking (Admin only)
     */
    @Transactional
    public void deleteBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + id));

        bookingRepository.delete(booking);
    }

    // ==================== DRIVER OPERATIONS (Own only) ====================

    /**
     * CREATE - Driver tạo booking cho xe của mình
     */
    @Transactional
    public Booking createMyBooking(BookingRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can create personal bookings");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        // Driver chỉ được tạo booking cho xe của chính mình
        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("You can only create bookings for your own vehicles");
        }

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));

        // Kiểm tra thời gian booking
        if (request.getBookingTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking time cannot be in the past");
        }

        Booking booking = new Booking();
        booking.setDriver(currentUser);
        booking.setVehicle(vehicle);
        booking.setStation(station);
        booking.setBookingTime(request.getBookingTime());
        booking.setStatus("Pending");

        return bookingRepository.save(booking);
    }

    /**
     * READ - Driver xem booking của mình
     */
    @Transactional(readOnly = true)
    public List<Booking> getMyBookings() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can view personal bookings");
        }
        return bookingRepository.findByDriverOrderByBookingTimeDesc(currentUser);
    }

    /**
     * READ - Driver xem booking cụ thể của mình
     */
    @Transactional(readOnly = true)
    public Booking getMyBookingById(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can view personal bookings");
        }

        return bookingRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Booking not found or access denied"));
    }

    /**
     * READ - Driver xem booking sắp tới của mình
     */
    @Transactional(readOnly = true)
    public List<Booking> getMyUpcomingBookings() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can view personal bookings");
        }
        return bookingRepository.findUpcomingByDriver(currentUser, LocalDateTime.now());
    }

    /**
     * UPDATE - Driver cập nhật booking của mình (chỉ trước khi confirmed)
     */
    @Transactional
    public Booking updateMyBooking(Long id, BookingRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can update personal bookings");
        }

        Booking booking = bookingRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Booking not found or access denied"));

        // Driver chỉ được update booking khi status là Pending
        if (!"Pending".equals(booking.getStatus())) {
            throw new AuthenticationException("You can only update bookings with Pending status");
        }

        // Kiểm tra vehicle ownership
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("You can only use your own vehicles");
        }

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + request.getStationId()));

        // Kiểm tra thời gian booking
        if (request.getBookingTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking time cannot be in the past");
        }

        booking.setVehicle(vehicle);
        booking.setStation(station);
        booking.setBookingTime(request.getBookingTime());

        return bookingRepository.save(booking);
    }

    /**
     * CANCEL - Driver hủy booking của mình
     */
    @Transactional
    public Booking cancelMyBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Only drivers can cancel personal bookings");
        }

        Booking booking = bookingRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Booking not found or access denied"));

        // Driver chỉ được cancel khi chưa completed
        if ("Completed".equals(booking.getStatus()) || "Cancelled".equals(booking.getStatus())) {
            throw new AuthenticationException("You can only cancel pending or confirmed bookings");
        }

        booking.setStatus("Cancelled");
        return bookingRepository.save(booking);
    }

    // ==================== STATUS FLOW OPERATIONS ====================

    /**
     * UPDATE STATUS - Staff cập nhật status (pending → confirmed → completed → cancelled)
     */
    @Transactional
    public Booking updateBookingStatus(Long id, String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + id));

        // Validate status transition
        if (!isValidStatusTransition(booking.getStatus(), status)) {
            throw new IllegalArgumentException("Invalid status transition from " + booking.getStatus() + " to " + status);
        }

        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    /**
     * FORCE CANCEL - Admin có thể force cancel booking
     */
    @Transactional
    public Booking forceCancelBooking(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + id));

        booking.setStatus("Cancelled");
        return bookingRepository.save(booking);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Lấy current bookings tại station (cho staff)
     */
    @Transactional(readOnly = true)
    public List<Booking> getCurrentBookingsByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + stationId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursLater = now.plusHours(2);

        return bookingRepository.findCurrentByStation(station, now, twoHoursLater);
    }

    /**
     * Lấy pending bookings tại station (cho staff)
     */
    @Transactional(readOnly = true)
    public List<Booking> getPendingBookingsByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + stationId));

        return bookingRepository.findByStationAndStatusOrderByBookingTimeAsc(station, "Pending");
    }

    /**
     * Validate status transition
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        return switch (currentStatus) {
            case "Pending" -> "Confirmed".equals(newStatus) || "Cancelled".equals(newStatus);
            case "Confirmed" -> "Completed".equals(newStatus) || "Cancelled".equals(newStatus);
            case "Completed", "Cancelled" -> false; // Final states
            default -> throw new IllegalArgumentException("Unknown current status: " + currentStatus);
        };
    }

    /**
     * Helper method kiểm tra role
     */
    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}