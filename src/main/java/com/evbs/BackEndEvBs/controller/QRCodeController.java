package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.service.QRCodeService;
import com.google.zxing.WriterException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Controller tạo QR Code cho trạm
 */
@RestController
@RequestMapping("/api/qr-code")
@SecurityRequirement(name = "api")
@Tag(name = "QR Code", description = "APIs tạo QR Code cho trạm đổi pin")
public class QRCodeController {

    @Autowired
    private QRCodeService qrCodeService;

    // Lấy URL frontend từ application.properties
    // Mặc định: http://103.200.20.190:5173
    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * GET /api/qr-code/station/{stationId} : Tạo QR Code cho trạm
     * 
     * Trả về hình ảnh QR code (PNG) chứa URL:
        /quick-swap?stationId={id}
     * 
     * Admin/Staff có thể download và in QR code để dán tại trạm
     */
    @GetMapping("/station/{stationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(
            summary = "Tạo QR Code cho trạm",
            description = "Tạo mã QR code cho trạm đổi pin. QR code chứa URL để khách hàng quét và đổi pin nhanh. " +
                    "Trả về hình ảnh PNG có thể download và in ra."
    )
    public ResponseEntity<byte[]> generateStationQRCode(
            @PathVariable Long stationId,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "300") int height
    ) {
        try {
            // Service xử lý TẤT CẢ logic: validation, tạo QR, tạo tên file
            QRCodeService.QRCodeResponse response = qrCodeService.generateStationQRCode(
                    stationId, frontendUrl, width, height);
            
            // Controller CHỈ xử lý HTTP response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("attachment", response.getFileName());
            
            return new ResponseEntity<>(response.getImageData(), headers, HttpStatus.OK);
            
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Lỗi khi tạo QR code: " + e.getMessage());
        }
    }
    
}
