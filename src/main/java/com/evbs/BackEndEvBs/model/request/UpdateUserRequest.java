package com.evbs.BackEndEvBs.model.request;

import com.evbs.BackEndEvBs.enity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    
    private String fullName;
    
    @Email(message = "Email invalid!")
    private String email;
    
    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Phone invalid!"
    )
    private String phoneNumber;
    
    // Không cho phép update password trực tiếp, sẽ có API riêng
    
    private User.Role role;
    
    private String status;
}