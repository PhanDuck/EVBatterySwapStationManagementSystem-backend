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
     * ⭐ POST /api/swap-transaction/swap-by-code : Driver tự swap bằng confirmation code
     * 
     * Driver nhập mã xác nhận (ABC123) tại trạm → Tự động tạo swap transaction
     */
    @PostMapping("/swap-by-code")
    @Operation(summary = "Self-service swap by confirmation code",
            description = "Driver nhập mã xác nhận 6 ký tự tại trạm để tự động swap pin")
    public ResponseEntity<SwapTransaction> swapByConfirmationCode(@RequestParam String confirmationCode) {
        SwapTransaction transaction = swapTransactionService.createSwapByConfirmationCode(confirmationCode);
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

    // ❌ REMOVED: Driver self-complete (security risk)
    // Transaction will be auto-completed after payment
    // Only staff can manually complete if needed

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
            @RequestParam SwapTransaction.Status status) {
        SwapTransaction transaction = swapTransactionService.updateTransactionStatus(id, status);
        return ResponseEntity.ok(transaction);
    }

    /**
     * ⭐ GET /api/swap-transaction/vehicle/{vehicleId}/history : Xem lịch sử đổi pin của xe
     * 
     * Staff/Admin: Xem được tất cả xe
     * Driver: Chỉ xem được xe của mình
     */
    @GetMapping("/vehicle/{vehicleId}/history")
    @Operation(summary = "Get vehicle swap history",
            description = "Xem lịch sử đổi pin của 1 xe cụ thể (Driver chỉ xem xe mình, Staff/Admin xem tất cả)")
    public ResponseEntity<List<SwapTransaction>> getVehicleSwapHistory(@PathVariable Long vehicleId) {
        List<SwapTransaction> history = swapTransactionService.getVehicleSwapHistory(vehicleId);
        return ResponseEntity.ok(history);
    }

    /**
     * 🔋 GET /api/swap-transaction/battery/{batteryId}/history : Xem lịch sử sử dụng của pin
     * 
     * Staff/Admin xem pin đã được dùng bởi driver nào, xe nào, ở đâu
     * Bao gồm cả swap OUT (lấy ra) và swap IN (trả về)
     */
    @GetMapping("/battery/{batteryId}/history")
    @Operation(summary = "Get battery usage history",
            description = "Xem lịch sử sử dụng của pin (bao gồm swap-out và swap-in) - Staff/Admin only")
    public ResponseEntity<List<SwapTransaction>> getBatteryUsageHistory(@PathVariable Long batteryId) {
        List<SwapTransaction> history = swapTransactionService.getBatteryUsageHistory(batteryId);
        return ResponseEntity.ok(history);
    }
}