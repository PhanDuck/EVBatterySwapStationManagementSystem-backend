package com.evbs.BackEndEvBs.exception;

import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
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
}

