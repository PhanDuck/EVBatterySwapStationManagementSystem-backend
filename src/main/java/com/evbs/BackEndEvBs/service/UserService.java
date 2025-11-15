package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.model.request.CreateUserRequest;
import com.evbs.BackEndEvBs.model.request.UpdateProfileRequest;
import com.evbs.BackEndEvBs.model.request.UpdateUserRequest;
import com.evbs.BackEndEvBs.model.response.UserResponse;
import com.evbs.BackEndEvBs.repository.BookingRepository;
import com.evbs.BackEndEvBs.repository.StaffStationAssignmentRepository;
import com.evbs.BackEndEvBs.repository.UserRepository;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    PasswordEncoder passwordEncoder;
    
    @Autowired
    VehicleRepository vehicleRepository;
    
    @Autowired
    BookingRepository bookingRepository;
    
    @Autowired
    StaffStationAssignmentRepository staffStationAssignmentRepository;

    /**
     * Tạo user mới
     */
    public UserResponse createUser(CreateUserRequest request) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        // Kiểm tra phone number đã tồn tại
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("SĐT đã được sử dụng!");
        }

        // Tạo user mới
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());

        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserResponse.class);
    }

    /**
     * Cập nhật thông tin user
     */
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        // Kiểm tra user hiện tại không được cập nhật chính mình
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getId().equals(id)) {
            throw new AuthenticationException("Không tự cập nhật!");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AuthenticationException("Không tìm thấy người dùng với id: " + id));

        // BẢO VỆ ADMIN ROLE
        if (user.getRole() == User.Role.ADMIN && request.getRole() != null && request.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Không được hạ cấp Admin!");
        }

        // Cập nhật các field nếu có giá trị mới
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            // Kiểm tra email mới có trùng với user khác không
            if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email đã được sử dụng!");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            // Kiểm tra phone number mới có trùng với user khác không
            if (!user.getPhoneNumber().equals(request.getPhoneNumber()) && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new IllegalArgumentException("SĐT đã được sử dụng!");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        // KHÔNG CHO ĐỔI THÀNH INACTIVE KHI CÓ XE/BOOKING ĐANG HOẠT ĐỘNG
        if (request.getStatus() != null && request.getStatus() == User.Status.INACTIVE) {
            if (user.getRole() == User.Role.DRIVER) {
                // Kiểm tra xe ACTIVE
                long activeVehicleCount = vehicleRepository.countByDriverAndStatus(user, Vehicle.VehicleStatus.ACTIVE);
                if (activeVehicleCount > 0) {
                    throw new IllegalStateException("Tài xế có xe hoạt động!");
                }
                
                // Kiểm tra booking CONFIRMED
                long confirmedBookingCount = bookingRepository.countByDriverAndStatus(user, Booking.Status.CONFIRMED);
                if (confirmedBookingCount > 0) {
                    throw new IllegalStateException("Tài xế có booking!");
                }
            }
        }
        
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        User updatedUser = userRepository.save(user);
        return modelMapper.map(updatedUser, UserResponse.class);
    }

    /**
     * Xóa user (soft delete bằng cách đổi status)
     */
    public void deleteUser(Long id) {
        // Kiểm tra user hiện tại không được xóa chính mình
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getId().equals(id)) {
            throw new AuthenticationException("Không tự xóa!");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AuthenticationException("Không tìm thấy người dùng với id: " + id));

        // KIỂM TRA THEO VAI TRÒ
        if (user.getRole() == User.Role.DRIVER) {
            // Kiểm tra xe ACTIVE
            long activeVehicleCount = vehicleRepository.countByDriverAndStatus(user, Vehicle.VehicleStatus.ACTIVE);
            if (activeVehicleCount > 0) {
                throw new IllegalStateException("Tài xế có xe hoạt động!");
            }
            
            // Kiểm tra booking CONFIRMED
            long confirmedBookingCount = bookingRepository.countByDriverAndStatus(user, Booking.Status.CONFIRMED);
            if (confirmedBookingCount > 0) {
                throw new IllegalStateException("Tài xế có booking!");
            }
        } else if (user.getRole() == User.Role.STAFF) {
            // Kiểm tra assignment
            long assignmentCount = staffStationAssignmentRepository.countByStaff(user);
            if (assignmentCount > 0) {
                throw new IllegalStateException("Staff đang quản lý trạm!");
            }
        }

        // Soft delete
        user.setStatus(User.Status.INACTIVE);
        userRepository.save(user);
    }

    /**
     * Lấy danh sách tất cả user đơn giản 
     */
    public List<UserResponse> getAllUsersSimple() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> modelMapper.map(user, UserResponse.class))
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật profile của chính user hiện tại
     */
    public UserResponse updateProfile(UpdateProfileRequest request) {
        // Lấy thông tin user hiện tại
        User currentUser = authenticationService.getCurrentUser();

        // Cập nhật các field nếu có giá trị mới
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            currentUser.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            // Kiểm tra email mới có trùng với user khác không
            if (!currentUser.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email đã được sử dụng!");
            }
            currentUser.setEmail(request.getEmail());
        }

        // Cập nhật ngày sinh nếu có
        if (request.getDateOfBirth() != null) {
            // Validate tuổi từ 16-100
            int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
            if (age < 16 || age > 100) {
                throw new IllegalArgumentException("Tuổi từ 16-100!");
            }
            currentUser.setDateOfBirth(request.getDateOfBirth());
        }

        // Cập nhật giới tính nếu có
        if (request.getGender() != null) {
            currentUser.setGender(request.getGender());
        }

        User updatedUser = userRepository.save(currentUser);
        return modelMapper.map(updatedUser, UserResponse.class);
    }
}