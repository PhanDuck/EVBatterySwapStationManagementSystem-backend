package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.StationRepository;
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

    @Autowired
    private StationRepository stationRepository;

    // Base URL của frontend app (có thể config trong application.properties)
    @Value("${app.frontend.url:https://app.evbs.com}")
    private String frontendUrl;

    /**
     * GET /api/qr-code/station/{stationId} : Tạo QR Code cho trạm
     * 
     * Trả về hình ảnh QR code (PNG) chứa URL:
     * https://app.evbs.com/quick-swap?stationId={id}
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
            // Kiểm tra trạm tồn tại
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm với ID: " + stationId));
            
            // Tạo QR code
            byte[] qrCode = qrCodeService.generateQRCode(stationId, frontendUrl, width, height);
            
            // Trả về hình ảnh PNG
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("attachment", 
                    "qr-code-station-" + stationId + "-" + station.getName() + ".png");
            
            return new ResponseEntity<>(qrCode, headers, HttpStatus.OK);
            
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Lỗi khi tạo QR code: " + e.getMessage());
        }
    }

    /**
     * GET /api/qr-code/station/{stationId}/preview : Xem trước QR Code (inline)
     * 
     * Hiển thị QR code trực tiếp trong browser (không download)
     */
    @GetMapping("/station/{stationId}/preview")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @Operation(
            summary = "Xem trước QR Code",
            description = "Xem trước QR code của trạm trong browser"
    )
    public ResponseEntity<byte[]> previewStationQRCode(
            @PathVariable Long stationId,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "300") int height
    ) {
        try {
            // Kiểm tra trạm tồn tại
            stationRepository.findById(stationId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm với ID: " + stationId));
            
            // Tạo QR code
            byte[] qrCode = qrCodeService.generateQRCode(stationId, frontendUrl, width, height);
            
            // Trả về hình ảnh để hiển thị inline
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            
            return new ResponseEntity<>(qrCode, headers, HttpStatus.OK);
            
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Lỗi khi tạo QR code: " + e.getMessage());
        }
    }
}
