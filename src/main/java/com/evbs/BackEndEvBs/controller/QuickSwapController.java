package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.SwapTransaction;
import com.evbs.BackEndEvBs.model.request.QuickSwapRequest;
import com.evbs.BackEndEvBs.model.response.QuickSwapPreviewResponse;
import com.evbs.BackEndEvBs.service.QuickSwapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý đổi pin nhanh qua QR Code
 * 
 * Flow đơn giản:
 * 1. Quét QR tại trạm → Lấy Station ID
 * 2. Chọn xe → GET /api/quick-swap/preview → Xem pin mới sẽ đổi
 * 3. Nhấn "Đổi pin" → POST /api/quick-swap/execute
 */
@RestController
@RequestMapping("/api/quick-swap")
@SecurityRequirement(name = "api")
@Tag(name = "Quick Swap", description = "APIs cho đổi pin nhanh qua QR Code")
public class QuickSwapController {

    @Autowired
    private QuickSwapService quickSwapService;

    /**
     * GET /api/quick-swap/preview : Xem pin mới sẽ đổi
     * 
     * Hiển thị:
     * - Thông tin trạm
     * - Pin mới sẽ đổi (charge, health)
     * - Lượt swap còn lại
     * - Có thể đổi hay không
     */
    @GetMapping("/preview")
    @PreAuthorize("hasRole('DRIVER') or hasRole('STAFF') or hasRole('ADMIN')")
    @Operation(
            summary = "Xem pin mới sẽ đổi",
            description = "Hiển thị thông tin pin mới sẽ được đổi tại trạm. Dùng sau khi quét QR code."
    )
    public ResponseEntity<QuickSwapPreviewResponse> previewQuickSwap(
            @RequestParam Long stationId,
            @RequestParam Long vehicleId
    ) {
        QuickSwapPreviewResponse preview = quickSwapService.previewQuickSwap(stationId, vehicleId);
        return ResponseEntity.ok(preview);
    }

    /**
     * POST /api/quick-swap/execute : Thực hiện đổi pin nhanh
     * 
     * DRIVER TỰ ĐỔI - Nhấn nút "Đổi pin" để thực hiện swap ngay lập tức
     * BẮT BUỘC đổi ĐÚNG pin đã hiển thị ở Preview
     */
    @PostMapping("/execute")
    @PreAuthorize("hasRole('DRIVER') or hasRole('STAFF') or hasRole('ADMIN')")
    @Operation(
            summary = "Thực hiện đổi pin nhanh",
            description = "Driver tự đổi pin ngay lập tức tại trạm. " +
                    "BẮT BUỘC đổi ĐÚNG pin đã hiển thị ở Preview (truyền batteryId). " +
                    "Tự động trừ lượt swap của driver."
    )
    public ResponseEntity<SwapTransaction> executeQuickSwap(
            @Valid @RequestBody QuickSwapRequest request
    ) {
        SwapTransaction transaction = quickSwapService.executeQuickSwap(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }
}
