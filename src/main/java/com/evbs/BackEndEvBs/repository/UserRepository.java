package com.evbs.BackEndEvBs.repository;


import com.evbs.BackEndEvBs.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Tìm user theo email
    Optional<User> findByEmail(String email);
    
    // Tìm user theo phone number
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    // Tìm user theo role
    List<User> findByRole(User.Role role);
    
    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);
    
    // Kiểm tra phone number đã tồn tại chưa
    boolean existsByPhoneNumber(String phoneNumber);
}