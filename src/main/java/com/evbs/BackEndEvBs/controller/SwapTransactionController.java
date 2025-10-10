package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.SwapTransaction;
import com.evbs.BackEndEvBs.model.request.SwapTransactionRequest;
import com.evbs.BackEndEvBs.service.SwapTransactionService;
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
@RequestMapping("/api/swap-transaction")
@SecurityRequirement(name = "api")
@Tag(name = "Swap Transaction Management")
public class SwapTransactionController {

    @Autowired
    private SwapTransactionService swapTransactionService;

    // ==================== DRIVER ENDPOINTS ====================

    /**
     * POST /api/swap-transaction : Create new swap transaction (Driver)
     */
    @PostMapping
    @Operation(summary = "Create new swap transaction")
    public ResponseEntity<SwapTransaction> createTransaction(@Valid @RequestBody SwapTransactionRequest request) {
        SwapTransaction transaction = swapTransactionService.createTransaction(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    /**
     * GET /api/swap-transaction/my-transactions : Get my transactions (Driver)
     */
    @GetMapping("/my-transactions")
    @Operation(summary = "Get my transactions")
    public ResponseEntity<List<SwapTransaction>> getMyTransactions() {
        List<SwapTransaction> transactions = swapTransactionService.getMyTransactions();
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/swap-transaction/my-transactions/{id} : Get my transaction by ID (Driver)
     */
    @GetMapping("/my-transactions/{id}")
    @Operation(summary = "Get my transaction by ID")
    public ResponseEntity<SwapTransaction> getMyTransaction(@PathVariable Long id) {
        SwapTransaction transaction = swapTransactionService.getMyTransaction(id);
        return ResponseEntity.ok(transaction);
    }

    /**
     * PATCH /api/swap-transaction/my-transactions/{id}/complete : Complete my transaction (Driver)
     */
    @PatchMapping("/my-transactions/{id}/complete")
    @Operation(summary = "Complete my transaction")
    public ResponseEntity<SwapTransaction> completeMyTransaction(@PathVariable Long id) {
        SwapTransaction transaction = swapTransactionService.completeMyTransaction(id);
        return ResponseEntity.ok(transaction);
    }

    // ==================== ADMIN/STAFF ENDPOINTS ====================

    /**
     * GET /api/swap-transaction : Get all transactions (Admin/Staff only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Get all transactions")
    public ResponseEntity<List<SwapTransaction>> getAllTransactions() {
        List<SwapTransaction> transactions = swapTransactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    /**
     * PUT /api/swap-transaction/{id} : Update transaction (Admin/Staff only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update transaction")
    public ResponseEntity<SwapTransaction> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody SwapTransactionRequest request) {
        SwapTransaction transaction = swapTransactionService.updateTransaction(id, request);
        return ResponseEntity.ok(transaction);
    }

    /**
     * PATCH /api/swap-transaction/{id}/status : Update transaction status (Admin/Staff only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(summary = "Update transaction status")
    public ResponseEntity<SwapTransaction> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        SwapTransaction transaction = swapTransactionService.updateTransactionStatus(id, status);
        return ResponseEntity.ok(transaction);
    }
}