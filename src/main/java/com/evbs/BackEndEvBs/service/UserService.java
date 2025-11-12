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
            throw new IllegalArgumentException("Email đã tồn tại!");
        }

        // Kiểm tra phone number đã tồn tại
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại!");
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
            throw new AuthenticationException("Bạn không thể cập nhật tài khoản của chính mình");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AuthenticationException("Không tìm thấy người dùng với id: " + id));

        // BẢO VỆ ADMIN ROLE
        if (user.getRole() == User.Role.ADMIN && request.getRole() != null && request.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Không thể hạ cấp Admin!");
        }

        // Cập nhật các field nếu có giá trị mới
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            // Kiểm tra email mới có trùng với user khác không
            if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email đã tồn tại!");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            // Kiểm tra phone number mới có trùng với user khác không
            if (!user.getPhoneNumber().equals(request.getPhoneNumber()) && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new IllegalArgumentException("Số điện thoại đã tồn tại!");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
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
            throw new AuthenticationException("Bạn không thể xóa tài khoản của chính mình");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AuthenticationException("Không tìm thấy người dùng với id: " + id));

        // KIỂM TRA THEO VAI TRÒ
        if (user.getRole() == User.Role.DRIVER) {
            // Kiểm tra xe ACTIVE
            long activeVehicleCount = vehicleRepository.countByDriverAndStatus(user, Vehicle.VehicleStatus.ACTIVE);
            if (activeVehicleCount > 0) {
                throw new IllegalStateException("Driver đang có " + activeVehicleCount + " xe ACTIVE, không thể xóa!");
            }
            
            // Kiểm tra booking CONFIRMED
            long confirmedBookingCount = bookingRepository.countByDriverAndStatus(user, Booking.Status.CONFIRMED);
            if (confirmedBookingCount > 0) {
                throw new IllegalStateException("Driver đang có " + confirmedBookingCount + " booking CONFIRMED, không thể xóa!");
            }
        } else if (user.getRole() == User.Role.STAFF) {
            // Kiểm tra assignment
            long assignmentCount = staffStationAssignmentRepository.countByStaff(user);
            if (assignmentCount > 0) {
                throw new IllegalStateException("Staff đang quản lý " + assignmentCount + " trạm, không thể xóa!");
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
                throw new IllegalArgumentException("Email đã tồn tại!");
            }
            currentUser.setEmail(request.getEmail());
        }

        // Cập nhật ngày sinh nếu có
        if (request.getDateOfBirth() != null) {
            // Validate tuổi từ 16-100
            int age = java.time.Period.between(request.getDateOfBirth(), java.time.LocalDate.now()).getYears();
            if (age < 16 || age > 100) {
                throw new IllegalArgumentException("Tuổi phải từ 16 đến 100 tuổi!");
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