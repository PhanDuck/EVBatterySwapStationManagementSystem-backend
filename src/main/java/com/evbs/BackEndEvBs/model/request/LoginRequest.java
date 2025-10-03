package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginRequest {

    @NotEmpty
    String phone;
    
    @NotEmpty
    String password;
}
