package com.evbs.BackEndEvBs.exception;

import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class APIExceptionHandler {

    // chạy mỗi khi mà dính lỗi
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus()
    public ResponseEntity handleBadRequest(MethodArgumentNotValidException exception) {
        String msg = "";
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            msg += error.getDefaultMessage() + "\n";
        }
        return ResponseEntity.badRequest().body(msg);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus()
    public ResponseEntity handleBadCredentialsException(BadCredentialsException exception) {
        return ResponseEntity.status(401).body("Số điện thoại hoặc mật khẩu không hợp lệ");
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity handleInternalAuthenticationServiceException(InternalAuthenticationServiceException exception) {
        return ResponseEntity.status(401).body("Số điện thoại hoặc mật khẩu không hợp lệ");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity handleAuthenticationException(AuthenticationException exception) {
        // Kiểm tra nếu là lỗi không được thao tác trên chính mình
        if (exception.getMessage().contains("Không thể cập nhật tài khoản của riêng bạn") ||
            exception.getMessage().contains("Không thể cập nhật tài khoản của riêng bạn")) {
            return ResponseEntity.status(403).body(exception.getMessage());
        }
        return ResponseEntity.status(401).body(exception.getMessage());
    }

    /**
     * Xử lý NotFoundException - 404 Not Found
     * Khi không tìm thấy resource (user, vehicle, booking, etc.)
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(NotFoundException exception) {
        return ResponseEntity.status(404).body(exception.getMessage());
    }

    /**
     * Xử lý IllegalStateException - 400 Bad Request
     * Khi business logic không hợp lệ (ví dụ: xóa xe đang có booking active)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException exception) {
        return ResponseEntity.status(400).body(exception.getMessage());
    }

    /**
     * Xử lý IllegalArgumentException - 400 Bad Request
     * Khi tham số không hợp lệ
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseEntity.status(400).body(exception.getMessage());
    }

    /**
     * Xử lý tất cả các exception chưa được handle - 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception exception) {
        // Log để debug
        System.err.println("Unhandled exception: " + exception.getClass().getName());
        exception.printStackTrace();
        return ResponseEntity.status(500).body("Lỗi hệ thống: " + exception.getMessage());
    }
}

