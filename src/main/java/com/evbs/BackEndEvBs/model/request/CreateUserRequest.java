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
    
    @NotEmpty(message = "FullName cannot be empty!")
    private String fullName;
    
    @Email(message = "Email invalid!")
    @NotEmpty(message = "Email cannot be empty!")
    private String email;
    
    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Phone invalid!"
    )
    private String phoneNumber;
    
    @NotEmpty(message = "Password cannot be empty!")
    private String password;
    
    private User.Role role = User.Role.DRIVER; // default
    
    private String status = "Active"; // default
}