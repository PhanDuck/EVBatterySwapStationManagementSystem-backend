package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.TicketResponse;
import com.evbs.BackEndEvBs.model.request.TicketResponseRequest;
import com.evbs.BackEndEvBs.service.TicketResponseService;
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
@RequestMapping("/api/ticket-response")
@SecurityRequirement(name = "api")
@Tag(name = "Ticket Response Management")
public class TicketResponseController {

    @Autowired
    private TicketResponseService ticketResponseService;

    // ==================== STAFF/ADMIN ENDPOINTS ====================

    /**
     * POST /api/ticket-response : Create response for ticket (Staff/Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Create response for ticket")
    public ResponseEntity<TicketResponse> createResponse(@Valid @RequestBody TicketResponseRequest request) {
        TicketResponse response = ticketResponseService.createResponse(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/ticket-response : Get all responses (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all responses")
    public ResponseEntity<List<TicketResponse>> getAllResponses() {
        List<TicketResponse> responses = ticketResponseService.getAllResponses();
        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/ticket-response/my-responses : Get my responses (Staff/Admin only)
     */
    @GetMapping("/my-responses")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get my responses")
    public ResponseEntity<List<TicketResponse>> getMyResponses() {
        List<TicketResponse> responses = ticketResponseService.getMyResponses();
        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/ticket-response/ticket/{ticketId} : Get responses by ticket (Admin/Staff only)
     */
    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get responses by ticket")
    public ResponseEntity<List<TicketResponse>> getResponsesByTicket(@PathVariable Long ticketId) {
        List<TicketResponse> responses = ticketResponseService.getResponsesByTicket(ticketId);
        return ResponseEntity.ok(responses);
    }

    /**
     * PUT /api/ticket-response/{id} : Update response (Staff/Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update response")
    public ResponseEntity<TicketResponse> updateResponse(
            @PathVariable Long id,
            @Valid @RequestBody TicketResponseRequest request) {
        TicketResponse response = ticketResponseService.updateResponse(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/ticket-response/{id} : Delete response (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete response")
    public ResponseEntity<Void> deleteResponse(@PathVariable Long id) {
        ticketResponseService.deleteResponse(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== DRIVER ENDPOINTS ====================

    /**
     * GET /api/ticket-response/my-ticket/{ticketId} : Get responses for my ticket (Driver)
     */
    @GetMapping("/my-ticket/{ticketId}")
    @Operation(summary = "Get responses for my ticket")
    public ResponseEntity<List<TicketResponse>> getResponsesForMyTicket(@PathVariable Long ticketId) {
        List<TicketResponse> responses = ticketResponseService.getResponsesForMyTicket(ticketId);
        return ResponseEntity.ok(responses);
    }
}