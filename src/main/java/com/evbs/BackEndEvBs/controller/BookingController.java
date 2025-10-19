package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.model.request.BookingRequest;
import com.evbs.BackEndEvBs.service.BookingService;
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
     */
    @PostMapping
    @Operation(summary = "Create new booking",
            description = "Create a new booking. System automatically validates battery type compatibility between vehicle and station.")
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
     */
    @PatchMapping("/my-bookings/{id}/cancel")
    @Operation(summary = "Cancel my booking",
            description = "Cancel a booking. Only PENDING bookings can be cancelled by driver.")
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
     * PATCH /api/booking/{id}/status : Update booking status (Admin/Staff only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update booking status",
            description = "Update booking status with validation for status transitions (Admin/Staff only)")
    public ResponseEntity<Booking> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam Booking.Status status) {
        Booking booking = bookingService.updateBookingStatus(id, status);
        return ResponseEntity.ok(booking);
    }

    /**
     * DELETE /api/booking/{id} : Delete booking (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete booking",
            description = "Permanently delete a booking (Admin only)")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== UTILITY ENDPOINTS ====================

    /**
     * GET /api/booking/station/{stationId} : Get bookings by station (Admin/Staff only)
     */
    @GetMapping("/station/{stationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get bookings by station",
            description = "Get all bookings for a specific station (Admin/Staff only)")
    public ResponseEntity<List<Booking>> getBookingsByStation(@PathVariable Long stationId) {
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

    // ==================== STAFF CONFIRMATION CODE ENDPOINTS ====================

    /**
     * PATCH /api/booking/{id}/confirm : Confirm booking by ID (Staff/Admin only)
     * 
     * Staff/Admin confirm booking → Generate mã xác nhận → Trả cho driver
     * PENDING → CONFIRMED (với confirmationCode mới)
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Confirm booking and generate confirmation code",
            description = "Staff/Admin xác nhận booking, hệ thống tự động tạo mã 6 ký tự (ABC123) cho driver")
    public ResponseEntity<Booking> confirmBooking(@PathVariable Long id) {
        Booking booking = bookingService.confirmBookingById(id);
        return ResponseEntity.ok(booking);
    }

    /**
     * GET /api/booking/verify/{confirmationCode} : Verify booking by confirmation code (Driver uses at station)
     * 
     * Driver nhập mã code (ABC123) vào máy tại trạm → Kiểm tra booking hợp lệ
     * Endpoint này có thể được gọi bởi Driver hoặc hệ thống tự động tại trạm
     */
    @GetMapping("/verify/{confirmationCode}")
    @Operation(summary = "Verify booking by confirmation code",
            description = "Driver nhập mã xác nhận 6 ký tự (ABC123) tại trạm để verify booking")
    public ResponseEntity<Booking> verifyBookingByCode(@PathVariable String confirmationCode) {
        Booking booking = bookingService.verifyBookingByCode(confirmationCode);
        return ResponseEntity.ok(booking);
    }

    /**
     * PATCH /api/booking/confirm/{confirmationCode} : Confirm booking by code (DEPRECATED)
     * 
     * @deprecated Use PATCH /api/booking/{id}/confirm instead
     */
    @Deprecated
    @PatchMapping("/confirm/{confirmationCode}")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "[DEPRECATED] Confirm booking by confirmation code",
            description = "Use PATCH /api/booking/{id}/confirm instead")
    public ResponseEntity<Booking> confirmBookingByCode(@PathVariable String confirmationCode) {
        Booking booking = bookingService.confirmBookingByCode(confirmationCode);
        return ResponseEntity.ok(booking);
    }
}
