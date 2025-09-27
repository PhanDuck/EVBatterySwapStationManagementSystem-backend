package com.example.EVBatterySwapStationManagementSystem.model.request;

import com.example.EVBatterySwapStationManagementSystem.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotEmpty(message = "FullName cannot be empty!")
    private String fullName;

    @Email(message = "Email invalid!")
    private String email;

    private String gender;

    @NotEmpty(message = "Password cannot be empty!")
    private String password;

    @Pattern(regexp = "^(03|05|07|08|09)[0-9]{8}$", message = "Phone invalid!")
    private String phone;
}