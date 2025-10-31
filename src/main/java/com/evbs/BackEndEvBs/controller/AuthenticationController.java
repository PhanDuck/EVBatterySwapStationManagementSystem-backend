package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.model.request.LoginRequest;
import com.evbs.BackEndEvBs.model.request.UpdatePasswordRequest;
import com.evbs.BackEndEvBs.model.response.UserResponse;
import com.evbs.BackEndEvBs.service.AuthenticationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@SecurityRequirement(name = "api")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/api/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        return ResponseEntity.ok(authenticationService.register(user));
    }

    @PostMapping("/api/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authenticationService.login(loginRequest));
    }

    @GetMapping("/api/current")
    public ResponseEntity<User> getCurrentUser() {
        return ResponseEntity.ok(authenticationService.getCurrentUser());
    }

    @PostMapping("/api/reset-password")
    public void resetPassword(@RequestParam String email) {
        authenticationService.resetPassword(email);
    }

    @PostMapping("/api/update-password")
    public ResponseEntity updatePassword(@RequestBody UpdatePasswordRequest updatePasswordRequest) {
        UserResponse user = authenticationService.updatePassword(updatePasswordRequest);
        return ResponseEntity.ok(user);
    }
}