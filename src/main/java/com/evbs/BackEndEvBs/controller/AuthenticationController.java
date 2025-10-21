package com.evbs.BackEndEvBs.controller;


import com.evbs.BackEndEvBs.entity.User;

import com.evbs.BackEndEvBs.model.request.LoginRequest;
import com.evbs.BackEndEvBs.model.response.UserResponse;
import com.evbs.BackEndEvBs.service.AuthenticationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@SecurityRequirement(name = "api")
public class AuthenticationController {
    //điều hướng

    @Autowired
    AuthenticationService authenticationService;

    @PostMapping("/api/register")
    public ResponseEntity register(@Valid @RequestBody User user){
        //nhận yêu cầu từ FE
        //Đẩy qua authenticationService

        User newuser = authenticationService.register(user);
        return ResponseEntity.ok(newuser);
    }



    @PostMapping("/api/login")
    public ResponseEntity login(@Valid @RequestBody LoginRequest loginRequest){
            UserResponse user = authenticationService.login(loginRequest);
            return ResponseEntity.ok(user);
    }

    @GetMapping("/api/Current")
    public ResponseEntity getCurrentUser(){
        return ResponseEntity.ok(authenticationService.getCurrentUser());
    }

}
