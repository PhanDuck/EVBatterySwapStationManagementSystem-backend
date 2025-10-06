package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.repository.VehicleRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class VehicleService {
    @Autowired
    VehicleRepository vehicleRepository;

    @Autowired
    AuthenticationService authenticationService;

    public Vehicle createVehicle(@Valid Vehicle vehicle) {
        // Kiểm tra trùng VIN (nếu cần thêm bảo vệ)
        if (vehicleRepository.existsByVin(vehicle.getVin())) {
            throw new RuntimeException("VIN already exists!");
        }

        // Kiểm tra trùng biển số (nếu cần thêm bảo vệ)
        if (vehicleRepository.existsByPlateNumber(vehicle.getPlateNumber())) {
            throw new RuntimeException("Plate number already exists!");
        }

        vehicle.setUser(authenticationService.getCurrentUser());
        return vehicleRepository.save(vehicle);
    }

}
