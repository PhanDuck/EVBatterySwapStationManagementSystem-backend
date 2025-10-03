package com.evbs.BackEndEvBs.model.response;

import com.evbs.BackEndEvBs.enity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private User.Role role;
    private String status = "Active";
    private String token;
}

