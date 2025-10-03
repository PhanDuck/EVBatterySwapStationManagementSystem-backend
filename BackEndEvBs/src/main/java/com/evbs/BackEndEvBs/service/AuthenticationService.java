package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.enity.User;
import com.evbs.BackEndEvBs.model.request.LoginRequest;
import com.evbs.BackEndEvBs.model.response.UserResponse;
import com.evbs.BackEndEvBs.repository.AuthenticationRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthenticationService implements UserDetailsService {


    //xử lí lgic của controller đưa qua
    @Autowired
    AuthenticationRepository authenticationRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    TokenService tokenService;

    public User register(User user){
        //xử lí logic cho register
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));


        //lưu vô database
         return authenticationRepository.save(user);
    }




    public UserResponse login(LoginRequest loginRequest){

        //xứ lí logic
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginRequest.getPhone(),
                    loginRequest.getPassword()
            ));
            User user = (User) authentication.getPrincipal();

            UserResponse userResponse = modelMapper.map(user, UserResponse.class);
            String token = tokenService.generateToken(user);
            userResponse.setToken(token);
            return userResponse;
    }




    public List<User> getAllUser(){
        List<User> users = authenticationRepository.findAll();
        return users;
    }

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        return authenticationRepository.findUserByPhoneNumber(phoneNumber);

    }

    public User getCurrentUser(){
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    //cơ chế
    //b1 lấy phone người dụng nhập
    //b2: tìm trong db xem có user nào trùng với phone đó không
    //b4 authenticationManager => compare tk password dưới db <=> pass người dùng nhập
}
