package com.evbs.BackEndEvBs.service;


import com.evbs.BackEndEvBs.entity.User;

import com.evbs.BackEndEvBs.model.EmailDetail;
import com.evbs.BackEndEvBs.model.request.LoginRequest;
import com.evbs.BackEndEvBs.model.request.UpdatePasswordRequest;
import com.evbs.BackEndEvBs.model.response.UserResponse;
import com.evbs.BackEndEvBs.repository.AuthenticationRepository;
import com.evbs.BackEndEvBs.repository.UserRepository;
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



@Service
public class AuthenticationService implements UserDetailsService {


    //xử lí lgic của controller đưa qua
    @Autowired
    AuthenticationRepository authenticationRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    TokenService tokenService;

    @Autowired
    EmailService emailService;

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


    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        return authenticationRepository.findUserByPhoneNumber(phoneNumber);
    }

    public User getCurrentUser(){
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }


    /**
     * QUÊN MẬT KHẨU: Gửi email reset password (không cần đăng nhập)
     */
    public boolean resetPassword(String email) {
        User user = authenticationRepository.findUserByEmail(email);

        if (user == null) return true;

        // Tạo token cho reset password được 15 thôi ehhehe
        String token = tokenService.generatePasswordResetToken(user);

        // Tạo URL reset password
        String url = "http://evbatteryswapsystem.com/reset-password?token=" + token;

        // Tạo email detail
        EmailDetail emailDetail = new EmailDetail();
        emailDetail.setRecipient(user.getEmail());
        emailDetail.setSubject("Reset Password - EV Battery Swap Station");
        emailDetail.setFullName(user.getFullName());
        emailDetail.setUrl(url);

        // Gửi email chứa link reset password
        emailService.sendPasswordResetEmail(emailDetail);

        return true;
    }

    public UserResponse updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        User user = getCurrentUser();
        user.setPasswordHash(passwordEncoder.encode(updatePasswordRequest.getPassword()));
        return modelMapper.map(authenticationRepository.save(user), UserResponse.class);
    }


}
