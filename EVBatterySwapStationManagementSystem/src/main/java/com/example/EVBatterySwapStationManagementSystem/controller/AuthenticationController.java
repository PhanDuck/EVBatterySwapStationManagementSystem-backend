package com.example.EVBatterySwapStationManagementSystem.controller;

import com.example.EVBatterySwapStationManagementSystem.entity.User;
import com.example.EVBatterySwapStationManagementSystem.model.request.LoginRequest;
import com.example.EVBatterySwapStationManagementSystem.model.request.RegisterRequest;
import com.example.EVBatterySwapStationManagementSystem.model.response.LoginResponse;
import com.example.EVBatterySwapStationManagementSystem.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User newUser = authenticationService.register(registerRequest);
            return ResponseEntity.ok(newUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authenticationService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
}