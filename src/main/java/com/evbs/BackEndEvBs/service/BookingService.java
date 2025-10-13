package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
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

    /**
     * CREATE - Tạo booking mới (Driver)
     */
    @Transactional
    public Booking createBooking(BookingRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        // Validate vehicle thuộc về driver
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));

        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Vehicle does not belong to current user");
        }

        // Validate station
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Station not found"));

        // Tạo booking thủ công thay vì dùng ModelMapper để tránh conflict
        Booking booking = new Booking();
        booking.setDriver(currentUser);
        booking.setVehicle(vehicle);
        booking.setStation(station);
        booking.setBookingTime(request.getBookingTime());

        return bookingRepository.save(booking);
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
    public Booking updateBookingStatus(Long id, Booking.Status status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        booking.setStatus(status);
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

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}