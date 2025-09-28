package com.example.EVBatterySwapStationManagementSystem.model.response;

import com.example.EVBatterySwapStationManagementSystem.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    private Long id;
    private String fullName;
    private String email;
    private String gender;
    private String phone;
    private String role;
    private String token;

    public static LoginResponse fromUser(User user, String token) {
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setGender(user.getGender());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole().name());
        response.setToken(token);
        return response;
    }
}