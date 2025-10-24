package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.SwapTransaction;
import com.evbs.BackEndEvBs.service.SwapTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/swap-transaction")
@Tag(name = "Swap Transaction Management")
// 🚨 QUAN TRỌNG: KHÔNG có @SecurityRequirement ở class level
public class SwapTransactionController {

    @Autowired
    private SwapTransactionService swapTransactionService;

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * POST /api/swap-transaction/swap-by-code : Driver tự swap bằng confirmation code (PUBLIC)
     */
    @PostMapping("/swap-by-code")
    @Operation(summary = "Self-service swap by confirmation code (Public)",
            description = "Driver nhập mã xác nhận 6 ký tự tại trạm để tự động swap pin. Không cần đăng nhập.")
    // 🎯 PUBLIC: Không có SecurityRequirement và PreAuthorize
    public ResponseEntity<SwapTransaction> swapByConfirmationCode(@RequestParam String confirmationCode) {
        SwapTransaction transaction = swapTransactionService.createSwapByConfirmationCode(confirmationCode);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    // ==================== DRIVER ENDPOINTS ====================

    @GetMapping("/my-transactions")
    @PreAuthorize("hasRole('DRIVER')")
    @SecurityRequirement(name = "api") //  THÊM security cho từng method
    @Operation(summary = "Get my transactions")
    public ResponseEntity<List<SwapTransaction>> getMyTransactions() {
        List<SwapTransaction> transactions = swapTransactionService.getMyTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/my-transactions/{id}")
    @PreAuthorize("hasRole('DRIVER')")
    @SecurityRequirement(name = "api") //  THÊM security cho từng method
    @Operation(summary = "Get my transaction by ID")
    public ResponseEntity<SwapTransaction> getMyTransaction(@PathVariable Long id) {
        SwapTransaction transaction = swapTransactionService.getMyTransaction(id);
        return ResponseEntity.ok(transaction);
    }

    // ==================== ADMIN/STAFF ENDPOINTS ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api") //  THÊM security cho từng method
    @Operation(summary = "Get all transactions")
    public ResponseEntity<List<SwapTransaction>> getAllTransactions() {
        List<SwapTransaction> transactions = swapTransactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/vehicle/{vehicleId}/history")
    @SecurityRequirement(name = "api") //  THÊM security cho từng method
    @Operation(summary = "Get vehicle swap history")
    public ResponseEntity<List<SwapTransaction>> getVehicleSwapHistory(@PathVariable Long vehicleId) {
        List<SwapTransaction> history = swapTransactionService.getVehicleSwapHistory(vehicleId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/battery/{batteryId}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api") //  THÊM security cho từng method
    @Operation(summary = "Get battery usage history")
    public ResponseEntity<List<SwapTransaction>> getBatteryUsageHistory(@PathVariable Long batteryId) {
        List<SwapTransaction> history = swapTransactionService.getBatteryUsageHistory(batteryId);
        return ResponseEntity.ok(history);
    }
}