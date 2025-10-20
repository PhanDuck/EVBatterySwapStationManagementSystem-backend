package com.evbs.BackEndEvBs.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * MoMo Payment Utility Class
 * Xử lý mã hóa và build query string cho MoMo API
 */
public class MoMoUtil {

    /**
     * Generate HMAC SHA256 signature
     * 
     * @param data Data to sign
     * @param secretKey Secret key
     * @return Hex string signature
     */
    public static String hmacSHA256(String data, String secretKey) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKeySpec);
            byte[] hash = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating HMAC SHA256", e);
        }
    }

    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Build raw signature string từ parameters
     * Format: key1=value1&key2=value2&...
     * 
     * @param params Parameters sorted by key
     * @return Raw signature string
     */
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

    /**
     * Generate random order ID
     * Format: timestamp + random 6 digits
     */
    public static String generateOrderId() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 900000) + 100000;
        return timestamp + "" + random;
    }

    /**
     * Generate random request ID
     * Format: timestamp + random 6 digits
     */
    public static String generateRequestId() {
        return generateOrderId();
    }
}
