package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.VehicleRequest;
import com.evbs.BackEndEvBs.model.request.VehicleUpdateRequest;
import com.evbs.BackEndEvBs.repository.BatteryTypeRepository;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
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
    private final BatteryTypeRepository batteryTypeRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    /**
     * Creates a new Vehicle
     */
    @Transactional
    public Vehicle createVehicle(VehicleRequest vehicleRequest) {
        // Validate VIN unique
        if (vehicleRepository.existsByVin(vehicleRequest.getVin())) {
            throw new AuthenticationException("VIN already exists!");
        }

        // Validate PlateNumber unique
        if (vehicleRepository.existsByPlateNumber(vehicleRequest.getPlateNumber())) {
            throw new AuthenticationException("Plate number already exists!");
        }

        // Validate battery type exists
        BatteryType batteryType = batteryTypeRepository.findById(vehicleRequest.getBatteryTypeId())
                .orElseThrow(() -> new NotFoundException("Battery type not found"));

        // ✅ Tạo vehicle mới thủ công thay vì dùng ModelMapper (tránh conflict)
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vehicleRequest.getVin());
        vehicle.setPlateNumber(vehicleRequest.getPlateNumber());
        vehicle.setModel(vehicleRequest.getModel());
        vehicle.setDriver(authenticationService.getCurrentUser());
        vehicle.setBatteryType(batteryType);

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

        // Driver có thể update battery type
        if (vehicleRequest.getBatteryTypeId() != null) {
            BatteryType batteryType = batteryTypeRepository.findById(vehicleRequest.getBatteryTypeId())
                    .orElseThrow(() -> new NotFoundException("Battery type not found"));
            existingVehicle.setBatteryType(batteryType);
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

        // Update battery type nếu có
        if (vehicleRequest.getBatteryTypeId() != null) {
            BatteryType batteryType = batteryTypeRepository.findById(vehicleRequest.getBatteryTypeId())
                    .orElseThrow(() -> new NotFoundException("Battery type not found"));
            existingVehicle.setBatteryType(batteryType);
        }

        // Update driver nếu có (chỉ admin/staff)
        if (vehicleRequest.getDriverId() != null) {
            // Cần thêm logic để lấy User từ driverId, nhưng hiện tại chưa có UserService inject
            // Có thể thêm UserRepository vào đây nếu cần
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