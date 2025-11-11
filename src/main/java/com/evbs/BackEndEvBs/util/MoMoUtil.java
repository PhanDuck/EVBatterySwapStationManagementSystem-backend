package com.evbs.BackEndEvBs.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public class MoMoUtil {


    // Tạo HMAC-SHA256 từ dữ liệu và secret key (dùng để tạo/kiểm tra signature)
    public static String hmacSHA256(String data, String secretKey) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKeySpec);
            byte[] hash = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Lỗi tạo HMAC SHA256 cho MoMo payment", e);
        }
    }

    // Chuyển mảng byte sang chuỗi hex (dùng để biểu diễn HMAC)
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // Tạo chuỗi raw signature: sort params theo key rồi nối key=value bằng '&'
    public static String buildRawSignature(Map<String, String> params) {
        // Sort parameters by key
        Map<String, String> sortedParams = new TreeMap<>(params);

        StringBuilder rawSignature = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (rawSignature.length() > 0) {
                rawSignature.append("&");
            }
            rawSignature.append(entry.getKey()).append("=").append(entry.getValue());
        }

        return rawSignature.toString();
    }

    // Tạo orderId: timestamp + 6 chữ số ngẫu nhiên
    public static String generateOrderId() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 900000) + 100000;
        return timestamp + "" + random;
    }

    // Tạo requestId (giống orderId)
    public static String generateRequestId() {
        return generateOrderId();
    }
}
