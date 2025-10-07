package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle,Long> {
    boolean existsByVin(String vin);
    boolean existsByPlateNumber(String plateNumber);

    List<Vehicle> findByDriver(User driver);

    List<Vehicle> findByDriverId(Long driverId);

    Optional<Vehicle> findByIdAndDriver(Long id, User driver);

    Optional<Vehicle> findByPlateNumber(String plateNumber);

}
