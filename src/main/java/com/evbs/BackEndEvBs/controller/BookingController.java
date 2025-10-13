package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Booking;
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
@Tag(name = "Booking Management")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // ==================== DRIVER ENDPOINTS ====================

    /**
     * POST /api/booking : Create new booking (Driver)
     */
    @PostMapping
    @Operation(summary = "Create new booking")
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.createBooking(request);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    /**
     * GET /api/booking/my-bookings : Get my bookings (Driver)
     */
    @GetMapping("/my-bookings")
    @Operation(summary = "Get my bookings")
    public ResponseEntity<List<Booking>> getMyBookings() {
        List<Booking> bookings = bookingService.getMyBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/booking/my-bookings/{id} : Get my booking by ID (Driver)
     */
    @GetMapping("/my-bookings/{id}")
    @Operation(summary = "Get my booking by ID")
    public ResponseEntity<Booking> getMyBooking(@PathVariable Long id) {
        Booking booking = bookingService.getMyBooking(id);
        return ResponseEntity.ok(booking);
    }

    /**
     * PATCH /api/booking/my-bookings/{id}/cancel : Cancel my booking (Driver)
     */
    @PatchMapping("/my-bookings/{id}/cancel")
    @Operation(summary = "Cancel my booking")
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
    @Operation(summary = "Get all bookings")
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * PATCH /api/booking/{id}/status : Update booking status (Admin/Staff only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update booking status")
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
    @Operation(summary = "Delete booking")
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
    @Operation(summary = "Get bookings by station")
    public ResponseEntity<List<Booking>> getBookingsByStation(@PathVariable Long stationId) {
        List<Booking> bookings = bookingService.getBookingsByStation(stationId);
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/booking/status/{status} : Get bookings by status (Admin/Staff only)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get bookings by status")
    public ResponseEntity<List<Booking>> getBookingsByStatus(@PathVariable Booking.Status status) {
        List<Booking> bookings = bookingService.getBookingsByStatus(status);
        return ResponseEntity.ok(bookings);
    }
}