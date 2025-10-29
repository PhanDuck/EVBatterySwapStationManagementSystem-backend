package com.evbs.BackEndEvBs.model.request;


import com.evbs.BackEndEvBs.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    
    private String fullName;
    
    @Email(message = "Email không hợp lệ!!")
    private String email;
    
    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Phone không hợp lệ!!"
    )
    private String phoneNumber;
    
    // Không cho phép update password trực tiếp, sẽ có API riêng
    
    private User.Role role;
    
    private User.Status status;
    private Long driverId; // Chỉ dành cho Admin có thể gán driver
}