package com.evbs.BackEndEvBs.model.request;

import com.evbs.BackEndEvBs.entity.User;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    
    private String fullName;
    
    @Email(message = "Email không hợp lệ!")
    private String email;
    
    private LocalDate dateOfBirth;
    
    private User.Gender gender;

}
