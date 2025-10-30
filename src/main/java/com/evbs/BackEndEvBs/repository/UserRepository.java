package com.evbs.BackEndEvBs.repository;


import com.evbs.BackEndEvBs.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);
    
    // Kiểm tra phone number đã tồn tại chưa
    boolean existsByPhoneNumber(String phoneNumber);

    // Dashboard queries - Đếm user theo role
    Long countByRole(User.Role role);

    // Đếm user theo status
    Long countByStatus(User.Status status);
}