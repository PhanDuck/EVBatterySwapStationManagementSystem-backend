package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.SupportTicket;
import com.evbs.BackEndEvBs.model.request.SupportTicketRequest;
import com.evbs.BackEndEvBs.service.SupportTicketService;
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
@RequestMapping("/api/support-ticket")
@SecurityRequirement(name = "api")
@Tag(name = "Support Ticket Management")
public class SupportTicketController {

    @Autowired
    private SupportTicketService supportTicketService;

    /**
     * POST /api/support-ticket : Create new ticket (Driver)
     */
    @PostMapping
    @Operation(summary = "Create new support ticket")
    public ResponseEntity<SupportTicket> createSupportTicket(@Valid @RequestBody SupportTicketRequest request) {
        SupportTicket ticket = supportTicketService.createSupportTicket(request);
        return new ResponseEntity<>(ticket, HttpStatus.CREATED);
    }

    /**
     * GET /api/support-ticket/my-tickets : Get my tickets (Driver)
     */
    @GetMapping("/my-tickets")
    @Operation(summary = "Get my tickets")
    public ResponseEntity<List<SupportTicket>> getMyTickets() {
        List<SupportTicket> tickets = supportTicketService.getMyTickets();
        return ResponseEntity.ok(tickets);
    }

    /**
     * GET /api/support-ticket/my-tickets/{id} : Get my ticket by ID (Driver)
     */
    @GetMapping("/my-tickets/{id}")
    @Operation(summary = "Get my ticket by ID")
    public ResponseEntity<SupportTicket> getMyTicket(@PathVariable Long id) {
        SupportTicket ticket = supportTicketService.getMyTicket(id);
        return ResponseEntity.ok(ticket);
    }

    /**
     * PUT /api/support-ticket/my-tickets/{id} : Update my ticket (Driver)
     */
    @PutMapping("/my-tickets/{id}")
    @Operation(summary = "Update my ticket")
    public ResponseEntity<SupportTicket> updateMyTicket(
            @PathVariable Long id,
            @Valid @RequestBody SupportTicketRequest request) {
        SupportTicket ticket = supportTicketService.updateMyTicket(id, request);
        return ResponseEntity.ok(ticket);
    }

    /**
     * DELETE /api/support-ticket/my-tickets/{id} : Delete my ticket (Driver)
     */
    @DeleteMapping("/my-tickets/{id}")
    @Operation(summary = "Delete my ticket")
    public ResponseEntity<Void> deleteMyTicket(@PathVariable Long id) {
        supportTicketService.deleteMyTicket(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/support-ticket : Get all tickets (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all tickets")
    public ResponseEntity<List<SupportTicket>> getAllTickets() {
        List<SupportTicket> tickets = supportTicketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    /**
     * PATCH /api/support-ticket/{id}/status : Update ticket status (Admin/Staff only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update ticket status")
    public ResponseEntity<SupportTicket> updateTicketStatus(
            @PathVariable Long id,
            @RequestParam SupportTicket.Status status) {
        SupportTicket ticket = supportTicketService.updateTicketStatus(id, status);
        return ResponseEntity.ok(ticket);
    }
}