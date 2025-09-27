package com.example.EVBatterySwapStationManagementSystem.repository;

import com.example.EVBatterySwapStationManagementSystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findUserByPhone(String phone);
    User findUserById(long id);
}