package com.evbs.BackEndEvBs.service;


import com.evbs.BackEndEvBs.entity.User;

import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.EmailDetail;
import com.evbs.BackEndEvBs.model.request.LoginRequest;
import com.evbs.BackEndEvBs.model.request.RegisterRequest;
import com.evbs.BackEndEvBs.model.request.UpdatePasswordRequest;
import com.evbs.BackEndEvBs.model.request.ChangePasswordRequest;
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

    @Autowired
    CaptchaService captchaService;

    public User register(RegisterRequest request){
        // Xác thực CAPTCHA trước
        if (!captchaService.verifyCaptcha(request.getCaptchaToken())) {
            throw new AuthenticationException("CAPTCHA không hợp lệ!");
        }

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        // Kiểm tra phone number đã tồn tại
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng!");
        }

        // Tạo user mới
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.DRIVER); // Mặc định DRIVER
        user.setStatus(User.Status.ACTIVE); // Mặc định ACTIVE

        // Lưu vô database
        return authenticationRepository.save(user);
    }


    public UserResponse login(LoginRequest loginRequest){
        // Xác thực CAPTCHA trước khi đăng nhập
        if (!captchaService.verifyCaptcha(loginRequest.getCaptchaToken())) {
            throw new AuthenticationException("CAPTCHA không hợp lệ!");
        }

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

        if (user == null) {
            throw new NotFoundException("Email không tồn tại!");
        }

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

    /**
     * ĐỔI MẬT KHẨU - Yêu cầu xác thực mật khẩu cũ
     */
    public UserResponse changePassword(ChangePasswordRequest request) {
        User currentUser = getCurrentUser();

        // 1. Kiểm tra mật khẩu cũ có đúng không
        if (!passwordEncoder.matches(request.getOldPassword(), currentUser.getPasswordHash())) {
            throw new AuthenticationException("Mật khẩu cũ sai!");
        }

        // 2. Kiểm tra mật khẩu mới và xác nhận mật khẩu có khớp không
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }

        // 3. Kiểm tra mật khẩu mới không được trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới trùng mật khẩu cũ!");
        }

        // 4. Cập nhật mật khẩu mới
        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        User updatedUser = authenticationRepository.save(currentUser);

        return modelMapper.map(updatedUser, UserResponse.class);
    }
}
