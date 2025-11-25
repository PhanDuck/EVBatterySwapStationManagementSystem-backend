package com.evbs.BackEndEvBs.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service tạo QR Code cho trạm
 */
@Service
@Slf4j
public class QRCodeService {

    /**
     * Tạo QR Code cho trạm
     * @param stationId ID của trạm
     * @param baseUrl URL gốc của app (ví dụ: https://app.evbs.com)
     * @param width Chiều rộng QR code (pixel)
     * @param height Chiều cao QR code (pixel)
     * @return Byte array của hình ảnh QR code (PNG)
     */
    public byte[] generateQRCode(Long stationId, String baseUrl, int width, int height) 
            throws WriterException, IOException {
        
        // URL sẽ encode vào QR: https://app.evbs.com/quick-swap?stationId=1
        String qrContent = baseUrl + "/quick-swap?stationId=" + stationId;
        
        log.info("Generating QR code for station {} with URL: {}", stationId, qrContent);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, width, height);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        return outputStream.toByteArray();
    }
    
    /**
     * Tạo QR Code với kích thước mặc định 300x300
     */
    public byte[] generateQRCode(Long stationId, String baseUrl) 
            throws WriterException, IOException {
        return generateQRCode(stationId, baseUrl, 300, 300);
    }
}
