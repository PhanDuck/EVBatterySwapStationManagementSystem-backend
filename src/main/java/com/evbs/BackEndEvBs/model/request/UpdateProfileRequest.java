package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    
    private String fullName;
    
    @Email(message = "Email không hợp lệ!")
    private String email;
    
    private String phoneNumber;
}
