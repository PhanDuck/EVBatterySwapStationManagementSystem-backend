package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.SwapTransaction;
import com.evbs.BackEndEvBs.model.response.BatteryInfoResponse;
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
public class SwapTransactionController {

    @Autowired
    private SwapTransactionService swapTransactionService;

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * GET /api/swap-transaction/old-battery : Xem thông tin pin CŨ (PUBLIC)
     */
    @GetMapping("/old-battery")
    @Operation(summary = "Get old battery information (Public)",
            description = "Xem thông tin pin CŨ đang lắp trên xe bằng confirmation code. Không cần đăng nhập.")
    public ResponseEntity<BatteryInfoResponse> getOldBatteryInfo(@RequestParam String code) {
        BatteryInfoResponse batteryInfo = swapTransactionService.getOldBatteryInfoByCode(code);
        return ResponseEntity.ok(batteryInfo);
    }

    /**
     * GET /api/swap-transaction/new-battery : Xem thông tin pin MỚI (PUBLIC)
     */
    @GetMapping("/new-battery")
    @Operation(summary = "Get new battery information (Public)",
            description = "Xem thông tin pin MỚI chuẩn bị lắp vào xe bằng confirmation code. Không cần đăng nhập.")
    public ResponseEntity<BatteryInfoResponse> getNewBatteryInfo(@RequestParam String code) {
        BatteryInfoResponse batteryInfo = swapTransactionService.getNewBatteryInfoByCode(code);
        return ResponseEntity.ok(batteryInfo);
    }

    /**
     * POST /api/swap-transaction/swap-by-code : Thực hiện đổi pin (PUBLIC)
     */
    @PostMapping("/swap-by-code")
    @Operation(summary = "Execute swap by confirmation code (Public)",
            description = "Thực hiện đổi pin bằng confirmation code. Không cần đăng nhập.")
    public ResponseEntity<SwapTransaction> swapByConfirmationCode(@RequestParam String code) {
        SwapTransaction transaction = swapTransactionService.createSwapByConfirmationCode(code);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    // ==================== DRIVER ENDPOINTS ====================

    @GetMapping("/my-transactions")
    @PreAuthorize("hasRole('DRIVER')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get my transactions")
    public ResponseEntity<List<SwapTransaction>> getMyTransactions() {
        List<SwapTransaction> transactions = swapTransactionService.getMyTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/my-transactions/{id}")
    @PreAuthorize("hasRole('DRIVER')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get my transaction by ID")
    public ResponseEntity<SwapTransaction> getMyTransaction(@PathVariable Long id) {
        SwapTransaction transaction = swapTransactionService.getMyTransaction(id);
        return ResponseEntity.ok(transaction);
    }

    // ==================== ADMIN/STAFF ENDPOINTS ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get all transactions")
    public ResponseEntity<List<SwapTransaction>> getAllTransactions() {
        List<SwapTransaction> transactions = swapTransactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/vehicle/{vehicleId}/history")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get vehicle swap history")
    public ResponseEntity<List<SwapTransaction>> getVehicleSwapHistory(@PathVariable Long vehicleId) {
        List<SwapTransaction> history = swapTransactionService.getVehicleSwapHistory(vehicleId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/battery/{batteryId}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @SecurityRequirement(name = "api")
    @Operation(summary = "Get battery usage history")
    public ResponseEntity<List<SwapTransaction>> getBatteryUsageHistory(@PathVariable Long batteryId) {
        List<SwapTransaction> history = swapTransactionService.getBatteryUsageHistory(batteryId);
        return ResponseEntity.ok(history);
    }
}