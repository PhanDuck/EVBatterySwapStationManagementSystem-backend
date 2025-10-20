package com.evbs.BackEndEvBs.util;

import java.security.SecureRandom;

/**
 * Utility để generate confirmation code 6 ký tự cho booking
 * Format: ABC123 (3 chữ cái in hoa + 3 số)
 */
public class ConfirmationCodeGenerator {

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBERS = "0123456789";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate confirmation code format: ABC123
     * - 3 ký tự chữ cái in hoa
     * - 3 ký tự số
     * 
     * @return Mã xác nhận 6 ký tự
     */
    public static String generate() {
        StringBuilder code = new StringBuilder(6);
        
        // 3 chữ cái đầu
        for (int i = 0; i < 3; i++) {
            code.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }
        
        // 3 số cuối
        for (int i = 0; i < 3; i++) {
            code.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        }
        
        return code.toString();
    }

    /**
     * Generate confirmation code với retry để đảm bảo unique
     * @param maxAttempts số lần thử tối đa
     * @param codeValidator function kiểm tra code đã tồn tại chưa
     * @return Mã xác nhận unique
     */
    public static String generateUnique(int maxAttempts, CodeValidator codeValidator) {
        for (int i = 0; i < maxAttempts; i++) {
            String code = generate();
            if (!codeValidator.exists(code)) {
                return code;
            }
        }
        throw new RuntimeException("Không thể generate unique confirmation code sau " + maxAttempts + " lần thử");
    }

    @FunctionalInterface
    public interface CodeValidator {
        boolean exists(String code);
    }
}
