package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service tạo QR Code cho trạm
 */
@Service
@Slf4j
public class QRCodeService {

    @Autowired
    private StationRepository stationRepository;
    
    /**
     * DTO để trả về QR code kèm metadata
     */
    @Data
    public static class QRCodeResponse {
        private byte[] imageData;
        private String fileName;
        private String stationName;
        
        public QRCodeResponse(byte[] imageData, String fileName, String stationName) {
            this.imageData = imageData;
            this.fileName = fileName;
            this.stationName = stationName;
        }
    }

    /**
     * Tạo QR Code cho trạm
     */
    public byte[] generateQRCode(Long stationId, String baseUrl, int width, int height) 
            throws WriterException, IOException {
        
        // URL sẽ encode vào QR: https://app.evbs.com/quick-swap?stationId=1
        String qrContent = baseUrl + "/quick-swap?stationId=" + stationId;
        
        log.info("Generating QR code for station {} with URL: {}", stationId, qrContent);
        
        //khởi tạo url thành bitmatrix, và bitmatrix là ma trận điểm ảnh của qr code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, width, height);
        
        //chuyển bitmatrix thành ảnh png nhờ thư viên MatrixToImageWriter của Google
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        return outputStream.toByteArray();
    }
    
    /**
     * Tạo QR Code cho trạm với validation và metadata
     * Method này chứa TẤT CẢ logic: validation, tạo QR, tạo tên file
     * Controller chỉ cần gọi và trả về response
     */
    public QRCodeResponse generateStationQRCode(Long stationId, String baseUrl, int width, int height) 
            throws WriterException, IOException {
        
        // 1. Validation: Kiểm tra trạm tồn tại
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm với ID: " + stationId));
        
        log.info("Tạo QR code cho trạm: {} - {}", station.getId(), station.getName());
        
        // 2. Tạo QR code
        byte[] qrCodeImage = generateQRCode(stationId, baseUrl, width, height);
        
        // 3. Tạo tên file
        String fileName = "qr-code-station-" + stationId + "-" + station.getName() + ".png";
        
        // 4. Trả về response với đầy đủ thông tin
        return new QRCodeResponse(qrCodeImage, fileName, station.getName());
    }
}
