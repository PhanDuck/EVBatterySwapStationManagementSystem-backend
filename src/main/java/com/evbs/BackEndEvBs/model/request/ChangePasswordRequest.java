package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    
    @NotEmpty(message = "Mật khẩu cũ không được để trống!")
    private String oldPassword;
    
    @NotEmpty(message = "Mật khẩu mới không được để trống!")
    @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự!")
    private String newPassword;
    
    @NotEmpty(message = "Xác nhận mật khẩu không được để trống!")
    private String confirmPassword;
}
