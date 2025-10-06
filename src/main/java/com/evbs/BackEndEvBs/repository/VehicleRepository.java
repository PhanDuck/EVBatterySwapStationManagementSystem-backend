package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle,Long> {
    boolean existsByVin(String vin);
    boolean existsByPlateNumber(String plateNumber);

    // Có thể thêm method tìm kiếm theo biển số nếu cần
    Optional<Vehicle> findByPlateNumber(String plateNumber);
}
