package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class VehicleService {
    @Autowired
    VehicleRepository vehicleRepository;

    @Autowired
    AuthenticationService authenticationService;

    public Vehicle createVehicle(@Valid Vehicle vehicle) {
        if (vehicleRepository.existsByVin(vehicle.getVin())) {
            throw new AuthenticationException("VIN already exists!");
        }

        if (vehicleRepository.existsByPlateNumber(vehicle.getPlateNumber())) {
            throw new AuthenticationException("Plate number already exists!");
        }

        vehicle.setDriver(authenticationService.getCurrentUser());
        return vehicleRepository.save(vehicle);
    }

    // READ - Lấy tất cả xe của user hiện tại (Driver)
    public List<Vehicle> getMyVehicles() {
        User currentUser = authenticationService.getCurrentUser();
        // SỬA: findByDriver thay vì findByUser
        return vehicleRepository.findByDriver(currentUser);
    }

    // READ - Lấy xe theo ID (Driver chỉ lấy được xe của mình)
    public Vehicle getMyVehicleById(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        // SỬA: findByIdAndDriver thay vì findByIdAndUser
        return vehicleRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new AuthenticationException("Vehicle not found or access denied"));
    }

    // READ - Lấy tất cả xe (Admin/Staff only)
    public List<Vehicle> getAllVehicles() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return vehicleRepository.findAll();
    }

    // READ - Lấy xe theo user ID (Admin/Staff only)
    public List<Vehicle> getVehiclesByUserId(Long userId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        // SỬA: findByDriverId thay vì findByUserId
        return vehicleRepository.findByDriverId(userId);
    }

    // UPDATE - Cập nhật thông tin không quan trọng (Driver)
    public Vehicle updateMyVehicle(Long id, Vehicle vehicleUpdate) {
        User currentUser = authenticationService.getCurrentUser();
        // SỬA: findByIdAndDriver thay vì findByIdAndUser
        Vehicle existingVehicle = vehicleRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new AuthenticationException("Vehicle not found or access denied"));

        // Driver chỉ được update model, không được thay đổi VIN, PlateNumber
        if (vehicleUpdate.getModel() != null && !vehicleUpdate.getModel().trim().isEmpty()) {
            existingVehicle.setModel(vehicleUpdate.getModel());
        }

        return vehicleRepository.save(existingVehicle);
    }

    // UPDATE - Cập nhật đầy đủ (Admin/Staff only)
    public Vehicle updateVehicle(Long id, Vehicle vehicleUpdate) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Vehicle existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new AuthenticationException("Vehicle not found"));

        // Admin/Staff được update tất cả thông tin
        if (vehicleUpdate.getModel() != null) {
            existingVehicle.setModel(vehicleUpdate.getModel());
        }

        // Kiểm tra trùng VIN nếu thay đổi
        if (vehicleUpdate.getVin() != null && !vehicleUpdate.getVin().equals(existingVehicle.getVin())) {
            if (vehicleRepository.existsByVin(vehicleUpdate.getVin())) {
                throw new AuthenticationException("VIN already exists!");
            }
            existingVehicle.setVin(vehicleUpdate.getVin());
        }

        // Kiểm tra trùng PlateNumber nếu thay đổi
        if (vehicleUpdate.getPlateNumber() != null && !vehicleUpdate.getPlateNumber().equals(existingVehicle.getPlateNumber())) {
            if (vehicleRepository.existsByPlateNumber(vehicleUpdate.getPlateNumber())) {
                throw new AuthenticationException("Plate number already exists!");
            }
            existingVehicle.setPlateNumber(vehicleUpdate.getPlateNumber());
        }

        // THÊM: Admin có thể thay đổi driver của vehicle
        if (vehicleUpdate.getDriver() != null) {
            existingVehicle.setDriver(vehicleUpdate.getDriver());
        }

        return vehicleRepository.save(existingVehicle);
    }

    // DELETE - Xóa xe (Admin/Staff only)
    public void deleteVehicle(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        if (!vehicleRepository.existsById(id)) {
            throw new AuthenticationException("Vehicle not found");
        }

        vehicleRepository.deleteById(id);
    }

    // THÊM: Helper method kiểm tra role
    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}
