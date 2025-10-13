package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.VehicleRequest;
import com.evbs.BackEndEvBs.model.request.VehicleUpdateRequest;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    @Autowired
    private final VehicleRepository vehicleRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    /**
     * Creates a new Vehicle
     */
    @Transactional
    public Vehicle createVehicle(VehicleRequest vehicleRequest) {
        if (vehicleRepository.existsByVin(vehicleRequest.getVin())) {
            throw new AuthenticationException("VIN already exists!");
        }

        if (vehicleRepository.existsByPlateNumber(vehicleRequest.getPlateNumber())) {
            throw new AuthenticationException("Plate number already exists!");
        }

        Vehicle vehicle = modelMapper.map(vehicleRequest, Vehicle.class);
        vehicle.setDriver(authenticationService.getCurrentUser());
        return vehicleRepository.save(vehicle);
    }

    /**
     * READ - Lấy vehicles của tôi (Driver only)
     */
    @Transactional(readOnly = true)
    public List<Vehicle> getMyVehicles() {
        User currentUser = authenticationService.getCurrentUser();
        return vehicleRepository.findByDriver(currentUser);
    }

    /**
     * READ - Lấy tất cả xe (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return vehicleRepository.findAll();
    }

    /**
     * UPDATE - Cập nhật thông tin không quan trọng (Driver)
     */
    @Transactional
    public Vehicle updateMyVehicle(Long id, VehicleUpdateRequest vehicleRequest) {
        User currentUser = authenticationService.getCurrentUser();
        Vehicle existingVehicle = vehicleRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Vehicle not found or access denied"));

        // Driver chỉ được update model, không được thay đổi VIN, PlateNumber
        if (vehicleRequest.getModel() != null && !vehicleRequest.getModel().trim().isEmpty()) {
            existingVehicle.setModel(vehicleRequest.getModel());
        }

        return vehicleRepository.save(existingVehicle);
    }

    /**
     * UPDATE - Cập nhật đầy đủ (Admin/Staff only)
     */
    @Transactional
    public Vehicle updateVehicle(Long id, VehicleUpdateRequest vehicleRequest) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Vehicle existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));

        // Admin/Staff được update tất cả thông tin
        if (vehicleRequest.getModel() != null) {
            existingVehicle.setModel(vehicleRequest.getModel());
        }

        // Kiểm tra trùng VIN nếu thay đổi
        if (vehicleRequest.getVin() != null && !vehicleRequest.getVin().equals(existingVehicle.getVin())) {
            if (vehicleRepository.existsByVin(vehicleRequest.getVin())) {
                throw new AuthenticationException("VIN already exists!");
            }
            existingVehicle.setVin(vehicleRequest.getVin());
        }

        // Kiểm tra trùng PlateNumber nếu thay đổi
        if (vehicleRequest.getPlateNumber() != null && !vehicleRequest.getPlateNumber().equals(existingVehicle.getPlateNumber())) {
            if (vehicleRepository.existsByPlateNumber(vehicleRequest.getPlateNumber())) {
                throw new AuthenticationException("Plate number already exists!");
            }
            existingVehicle.setPlateNumber(vehicleRequest.getPlateNumber());
        }

        return vehicleRepository.save(existingVehicle);
    }

    /**
     * DELETE - Xóa xe (Admin/Staff only)
     */
    @Transactional
    public void deleteVehicle(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle not found with id: " + id));
        vehicleRepository.delete(vehicle);
    }

    /**
     * Helper method kiểm tra role
     */
    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}