package com.evbs.BackEndEvBs.model.request;


import com.evbs.BackEndEvBs.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    
    @NotEmpty(message = "FullName không được để trống!")
    private String fullName;
    
    @Email(message = "Email không hợp lệ")
    @NotEmpty(message = "Email không được để trống!")
    private String email;
    
    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Số điện thoại không hợp lệ!"
    )
    private String phoneNumber;
    
    @NotEmpty(message = "Mật khẩu không được để trống!")
    private String password;
    
    private User.Role role = User.Role.DRIVER; // default
    
    private User.Status status = User.Status.ACTIVE; // default
}