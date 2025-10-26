package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.model.request.BookingRequest;
import com.evbs.BackEndEvBs.service.BookingService;
import com.evbs.BackEndEvBs.service.StaffStationAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking")
@SecurityRequirement(name = "api")
@Tag(name = "Booking Management", description = "APIs for managing bookings and compatible stations")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private StaffStationAssignmentService staffStationAssignmentService;

    // ==================== COMPATIBILITY ENDPOINTS ====================

    /**
     * GET /api/booking/compatible-stations/{vehicleId} : Get compatible stations for vehicle (Public)
     * Used to populate station dropdown when creating booking
     */
    @GetMapping("/compatible-stations/{vehicleId}")
    @Operation(summary = "Get compatible stations for vehicle",
            description = "Get list of stations that support the battery type of the selected vehicle. Used to populate station dropdown when creating booking.")
    public ResponseEntity<List<Station>> getCompatibleStations(@PathVariable Long vehicleId) {
        List<Station> stations = bookingService.getCompatibleStations(vehicleId);
        return ResponseEntity.ok(stations);
    }

    // ==================== DRIVER ENDPOINTS ====================

    /**
     * POST /api/booking : Create new booking (Driver)
     * System automatically sets booking time to 3 hours from now
     */
    @PostMapping
    @Operation(summary = "Create new booking",
            description = "Create a new booking. System automatically sets booking time to 3 hours from now and validates battery type compatibility.")
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.createBooking(request);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    /**
     * GET /api/booking/my-bookings : Get my bookings (Driver)
     */
    @GetMapping("/my-bookings")
    @Operation(summary = "Get my bookings",
            description = "Get all bookings for the current driver")
    public ResponseEntity<List<Booking>> getMyBookings() {
        List<Booking> bookings = bookingService.getMyBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/booking/my-bookings/{id} : Get my booking by ID (Driver)
     */
    @GetMapping("/my-bookings/{id}")
    @Operation(summary = "Get my booking by ID",
            description = "Get specific booking details for the current driver")
    public ResponseEntity<Booking> getMyBooking(@PathVariable Long id) {
        Booking booking = bookingService.getMyBooking(id);
        return ResponseEntity.ok(booking);
    }

    /**
     * PATCH /api/booking/my-bookings/{id}/cancel : Cancel my booking (Driver)
     * Only allowed to cancel before 1 hour of booking time
     */
    @PatchMapping("/my-bookings/{id}/cancel")
    @Operation(summary = "Cancel my booking",
            description = "Cancel a booking. Only allowed to cancel before 1 hour of booking time.")
    public ResponseEntity<Booking> cancelMyBooking(@PathVariable Long id) {
        Booking booking = bookingService.cancelMyBooking(id);
        return ResponseEntity.ok(booking);
    }

    // ==================== ADMIN/STAFF ENDPOINTS ====================

    /**
     * GET /api/booking : Get all bookings (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all bookings",
            description = "Get all bookings in the system (Admin/Staff only)")
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/booking/station/{stationId} : Get bookings by station (Admin/Staff only)
     * Staff chỉ xem được bookings của stations mình quản lý
     */
    @GetMapping("/station/{stationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get bookings by station",
            description = "Get all bookings for a specific station. Staff can only view bookings for their assigned stations.")
    public ResponseEntity<List<Booking>> getBookingsByStation(@PathVariable Long stationId) {
        // Validate station access for staff
        staffStationAssignmentService.validateStationAccess(stationId);

        List<Booking> bookings = bookingService.getBookingsByStation(stationId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/booking/status/{status} : Get bookings by status (Admin/Staff only)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get bookings by status",
            description = "Get all bookings with specific status (Admin/Staff only)")
    public ResponseEntity<List<Booking>> getBookingsByStatus(@PathVariable Booking.Status status) {
        List<Booking> bookings = bookingService.getBookingsByStatus(status);
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/booking/my-stations : Get bookings cua cac tram Staff quan ly (Staff only)
     *
     * Staff chi xem duoc bookings cua cac tram minh duoc assign
     * Admin xem duoc tat ca bookings
     *
     * Dung de hien thi danh sach booking can confirm
     */
    @GetMapping("/my-stations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get bookings for my managed stations (Staff only)",
            description = "Staff xem bookings cua cac tram minh quan ly. Admin xem tat ca bookings.")
    public ResponseEntity<List<Booking>> getBookingsForMyStations() {
        List<Booking> bookings = bookingService.getBookingsForMyStations();
        return ResponseEntity.ok(bookings);
    }

    // ==================== STAFF CONFIRMATION CODE ENDPOINTS ====================

    /**
     * DELETE /api/booking/staff/{id}/cancel : Cancel booking by Staff/Admin (Special cases)
     *
     * Staff/Admin co the huy bat ky booking nao (PENDING hoac CONFIRMED)
     * Truong hop dac biet: Tram bao tri, pin hong, khan cap, etc.
     *
     * Neu huy CONFIRMED booking:
     * - Giai phong pin ve AVAILABLE
     * - KHONG TRU luot swap cua driver (loi tu phia tram)
     */
    @DeleteMapping("/staff/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Cancel booking by Staff/Admin (Special cases)",
            description = "Staff/Admin huy booking trong truong hop dac biet (tram bao tri, pin hong, khan cap). " +
                    "Neu huy CONFIRMED booking se giai phong pin va KHONG tru luot swap cua driver.")
    public ResponseEntity<?> cancelBookingByStaff(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Booking booking = bookingService.cancelBookingByStaff(id, reason);
        return ResponseEntity.ok(booking);
    }
}