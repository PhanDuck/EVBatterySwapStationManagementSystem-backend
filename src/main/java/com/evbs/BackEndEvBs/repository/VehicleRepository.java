package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle,Long> {
    boolean existsByVin(String vin);
    boolean existsByPlateNumber(String plateNumber);

    // Kiểm tra VIN trùng chỉ trong xe ACTIVE hoặc PENDING (bỏ qua INACTIVE)
    boolean existsByVinAndStatusIn(String vin, List<Vehicle.VehicleStatus> statuses);

    // Kiểm tra biển số trùng chỉ trong xe ACTIVE hoặc PENDING (bỏ qua INACTIVE)
    boolean existsByPlateNumberAndStatusIn(String plateNumber, List<Vehicle.VehicleStatus> statuses);

    Optional<Vehicle> findByIdAndDriver(Long id, User driver);

    // Tìm tất cả vehicles của một driver
    List<Vehicle> findByDriver(User driver);

    // Tìm vehicles theo driver và status
    List<Vehicle> findByDriverAndStatus(User driver, Vehicle.VehicleStatus status);
    
    // Đếm số xe ACTIVE của driver
    long countByDriverAndStatus(User driver, Vehicle.VehicleStatus status);
    
    // Đếm số xe theo loại BatteryType (kiểm tra khi xóa BatteryType)
    long countByBatteryType_Id(Long batteryTypeId);

    // Tìm xe PENDING quá thời gian chờ duyệt (12 tiếng)
    List<Vehicle> findByStatusAndCreatedAtBefore(Vehicle.VehicleStatus status, LocalDateTime createdBefore);
}