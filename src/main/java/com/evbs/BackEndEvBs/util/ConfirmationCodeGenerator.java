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

    // Sinh mã 6 ký tự, ví dụ: ABC123
    public static String generate() {
        StringBuilder code = new StringBuilder(6);


        for (int i = 0; i < 3; i++) {
            code.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }


        for (int i = 0; i < 3; i++) {
            code.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        }

        return code.toString();
    }


    // Sinh mã unique với số lần thử tối đa
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
        // Kiểm tra mã đã tồn tại chưa (true = đã tồn tại)
        boolean exists(String code);
    }
}
