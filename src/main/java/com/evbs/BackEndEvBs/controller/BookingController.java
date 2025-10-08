package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.model.request.BookingRequest;
import com.evbs.BackEndEvBs.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Booking Management", description = "APIs for managing bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // ==================== FULL CRUD ENDPOINTS (Admin/Staff) ====================

    /**
     * POST /api/booking : Create new booking (Admin/Staff/Driver)
     */
    @PostMapping
    @Operation(summary = "Create new booking", description = "Create a new booking (Admin/Staff/Driver)")
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.createBooking(request);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    /**
     * GET /api/booking : Get all bookings (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all bookings", description = "Get list of all bookings (Admin/Staff only)")
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/booking/{id} : Get booking by ID (Admin/Staff only)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get booking by ID", description = "Get booking details by ID (Admin/Staff only)")
    public ResponseEntity<Booking> getBookingById(
            @Parameter(description = "Booking ID") @PathVariable Long id) {
        Booking booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    /**
     * GET /api/booking/station/{stationId} : Get bookings by station (Admin/Staff only)
     */
    @GetMapping("/station/{stationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get bookings by station", description = "Get bookings by station ID (Admin/Staff only)")
    public ResponseEntity<List<Booking>> getBookingsByStation(
            @Parameter(description = "Station ID") @PathVariable Long stationId) {
        List<Booking> bookings = bookingService.getBookingsByStation(stationId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/booking/status/{status} : Get bookings by status (Admin/Staff only)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get bookings by status", description = "Get bookings by status (Admin/Staff only)")
    public ResponseEntity<List<Booking>> getBookingsByStatus(
            @Parameter(description = "Booking status") @PathVariable String status) {
        List<Booking> bookings = bookingService.getBookingsByStatus(status);
        return ResponseEntity.ok(bookings);
    }

    /**
     * DELETE /api/booking/{id} : Delete booking (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete booking", description = "Delete booking (Admin only)")
    public ResponseEntity<Void> deleteBooking(
            @Parameter(description = "Booking ID") @PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== DRIVER ENDPOINTS (Own only) ====================

    /**
     * POST /api/booking/my-bookings : Create my booking (Driver only)
     */
    @PostMapping("/my-bookings")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Create my booking", description = "Create a new booking for current driver")
    public ResponseEntity<Booking> createMyBooking(@Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.createMyBooking(request);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    /**
     * GET /api/booking/my-bookings : Get my bookings (Driver only)
     */
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get my bookings", description = "Get bookings for current driver")
    public ResponseEntity<List<Booking>> getMyBookings() {
        List<Booking> bookings = bookingService.getMyBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/booking/my-bookings/{id} : Get my booking by ID (Driver only)
     */
    @GetMapping("/my-bookings/{id}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get my booking by ID", description = "Get specific booking for current driver")
    public ResponseEntity<Booking> getMyBookingById(
            @Parameter(description = "Booking ID") @PathVariable Long id) {
        Booking booking = bookingService.getMyBookingById(id);
        return ResponseEntity.ok(booking);
    }

    /**
     * GET /api/booking/my-bookings/upcoming : Get my upcoming bookings (Driver only)
     */
    @GetMapping("/my-bookings/upcoming")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get my upcoming bookings", description = "Get upcoming bookings for current driver")
    public ResponseEntity<List<Booking>> getMyUpcomingBookings() {
        List<Booking> bookings = bookingService.getMyUpcomingBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * PUT /api/booking/my-bookings/{id} : Update my booking (Driver only)
     */
    @PutMapping("/my-bookings/{id}")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update my booking", description = "Update booking for current driver")
    public ResponseEntity<Booking> updateMyBooking(
            @Parameter(description = "Booking ID") @PathVariable Long id,
            @Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.updateMyBooking(id, request);
        return ResponseEntity.ok(booking);
    }

    /**
     * PATCH /api/booking/my-bookings/{id}/cancel : Cancel my booking (Driver only)
     */
    @PatchMapping("/my-bookings/{id}/cancel")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Cancel my booking", description = "Cancel booking for current driver")
    public ResponseEntity<Booking> cancelMyBooking(
            @Parameter(description = "Booking ID") @PathVariable Long id) {
        Booking booking = bookingService.cancelMyBooking(id);
        return ResponseEntity.ok(booking);
    }

    // ==================== STATUS FLOW ENDPOINTS ====================

    /**
     * PATCH /api/booking/{id}/status : Update booking status (Admin/Staff only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update booking status", description = "Update booking status (Admin/Staff only)")
    public ResponseEntity<Booking> updateBookingStatus(
            @Parameter(description = "Booking ID") @PathVariable Long id,
            @RequestParam String status) {
        Booking booking = bookingService.updateBookingStatus(id, status);
        return ResponseEntity.ok(booking);
    }

    /**
     * PATCH /api/booking/{id}/force-cancel : Force cancel booking (Admin only)
     */
    @PatchMapping("/{id}/force-cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force cancel booking", description = "Force cancel booking (Admin only)")
    public ResponseEntity<Booking> forceCancelBooking(
            @Parameter(description = "Booking ID") @PathVariable Long id) {
        Booking booking = bookingService.forceCancelBooking(id);
        return ResponseEntity.ok(booking);
    }

    // ==================== UTILITY ENDPOINTS ====================

    /**
     * GET /api/booking/station/{stationId}/current : Get current bookings by station (Admin/Staff only)
     */
    @GetMapping("/station/{stationId}/current")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get current bookings by station", description = "Get current bookings at station (Admin/Staff only)")
    public ResponseEntity<List<Booking>> getCurrentBookingsByStation(
            @Parameter(description = "Station ID") @PathVariable Long stationId) {
        List<Booking> bookings = bookingService.getCurrentBookingsByStation(stationId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/booking/station/{stationId}/pending : Get pending bookings by station (Admin/Staff only)
     */
    @GetMapping("/station/{stationId}/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get pending bookings by station", description = "Get pending bookings at station (Admin/Staff only)")
    public ResponseEntity<List<Booking>> getPendingBookingsByStation(
            @Parameter(description = "Station ID") @PathVariable Long stationId) {
        List<Booking> bookings = bookingService.getPendingBookingsByStation(stationId);
        return ResponseEntity.ok(bookings);
    }
}