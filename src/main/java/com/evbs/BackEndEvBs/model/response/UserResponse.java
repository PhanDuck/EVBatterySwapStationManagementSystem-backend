package com.evbs.BackEndEvBs.model.response;


import com.evbs.BackEndEvBs.entity.User;



import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private User.Role role;
    private String status;
    
    // Token chỉ có khi login, null khi CRUD operations
    private String token;
}

