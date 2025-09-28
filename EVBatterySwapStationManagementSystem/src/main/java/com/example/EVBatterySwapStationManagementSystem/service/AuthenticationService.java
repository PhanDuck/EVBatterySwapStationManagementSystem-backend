package com.example.EVBatterySwapStationManagementSystem.service;

import com.example.EVBatterySwapStationManagementSystem.entity.User;
import com.example.EVBatterySwapStationManagementSystem.model.request.LoginRequest;
import com.example.EVBatterySwapStationManagementSystem.model.request.RegisterRequest;
import com.example.EVBatterySwapStationManagementSystem.model.response.LoginResponse;
import com.example.EVBatterySwapStationManagementSystem.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository; // Đảm bảo đúng tên

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TokenService tokenService;

    // Đăng ký tài khoản mới
    public User register(RegisterRequest registerRequest) {
        // Check if phone already exists
        if (userRepository.findUserByPhone(registerRequest.getPhone()) != null) {
            throw new RuntimeException("Phone number already registered!");
        }

        // Map RegisterRequest to User entity
        User user = modelMapper.map(registerRequest, User.class);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        return userRepository.save(user);
    }

    // Đăng nhập
    public LoginResponse login(LoginRequest loginRequest) {
        // Xác thực thông tin đăng nhập
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhone(),
                        loginRequest.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
        String token = tokenService.generateToken(user);

        return LoginResponse.fromUser(user, token);
    }

    // Load user by phone (dùng cho Spring Security)
    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userRepository.findUserByPhone(phone);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with phone: " + phone);
        }
        return user;
    }
}