package com.example.EVBatterySwapStationManagementSystem.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginRequest {
    @NotEmpty
    private String phone;

    @NotEmpty
    private String password;
}