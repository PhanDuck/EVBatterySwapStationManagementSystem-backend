package com.evbs.BackEndEvBs.controller;

import com.evbs.BackEndEvBs.enity.User;
import com.evbs.BackEndEvBs.model.request.LoginRequest;
import com.evbs.BackEndEvBs.model.response.UserResponse;
import com.evbs.BackEndEvBs.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AuthenticationController {
    //điều hướng

    @Autowired
    AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity register(@Valid @RequestBody User user){
        //nhận yêu cầu từ FE
        //Đẩy qua authenticationService

        User newuser = authenticationService.register(user);
        return ResponseEntity.ok(newuser);
    }

    @GetMapping("/getall")
    public ResponseEntity getAllUser(){
        List<User> users = authenticationService.getAllUser();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/login")
    public ResponseEntity login(@Valid @RequestBody LoginRequest loginRequest){
            UserResponse  user = authenticationService.login(loginRequest);
            return ResponseEntity.ok(user);

    }

}
