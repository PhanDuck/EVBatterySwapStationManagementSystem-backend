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
     * GET /api/ticket-response/ticket/{ticketId} : Get responses by ticket (Admin/Staff only)
     */
    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get responses by ticket")
    public ResponseEntity<List<TicketResponse>> getResponsesByTicket(@PathVariable Long ticketId) {
        List<TicketResponse> responses = ticketResponseService.getResponsesByTicket(ticketId);
        return ResponseEntity.ok(responses);
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