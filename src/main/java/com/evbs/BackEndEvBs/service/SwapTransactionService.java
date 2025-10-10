package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.*;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.SwapTransactionRequest;
import com.evbs.BackEndEvBs.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SwapTransactionService {

    @Autowired
    private final SwapTransactionRepository swapTransactionRepository;

    @Autowired
    private final VehicleRepository vehicleRepository;

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final BatteryRepository batteryRepository;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    /**
     * CREATE - Tạo transaction mới (Driver)
     */
    @Transactional
    public SwapTransaction createTransaction(SwapTransactionRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        // Validate vehicle thuộc về driver
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));

        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Vehicle does not belong to current user");
        }

        // Validate station
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Station not found"));

        // Validate staff
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        if (staff.getRole() != User.Role.STAFF && staff.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("User is not staff or admin");
        }

        // Validate batteries
        Battery swapOutBattery = null;
        Battery swapInBattery = null;

        if (request.getSwapOutBatteryId() != null) {
            swapOutBattery = batteryRepository.findById(request.getSwapOutBatteryId())
                    .orElseThrow(() -> new NotFoundException("Swap-out battery not found"));
        }

        if (request.getSwapInBatteryId() != null) {
            swapInBattery = batteryRepository.findById(request.getSwapInBatteryId())
                    .orElseThrow(() -> new NotFoundException("Swap-in battery not found"));
        }

        SwapTransaction transaction = modelMapper.map(request, SwapTransaction.class);
        transaction.setDriver(currentUser);
        transaction.setVehicle(vehicle);
        transaction.setStation(station);
        transaction.setStaff(staff);
        transaction.setSwapOutBattery(swapOutBattery);
        transaction.setSwapInBattery(swapInBattery);
        transaction.setStartTime(LocalDateTime.now());

        return swapTransactionRepository.save(transaction);
    }

    /**
     * READ - Lấy transactions của driver hiện tại
     */
    @Transactional(readOnly = true)
    public List<SwapTransaction> getMyTransactions() {
        User currentUser = authenticationService.getCurrentUser();
        return swapTransactionRepository.findByDriver(currentUser);
    }

    /**
     * READ - Lấy transaction cụ thể của driver
     */
    @Transactional(readOnly = true)
    public SwapTransaction getMyTransaction(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        return swapTransactionRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }

    /**
     * UPDATE - Hoàn thành transaction (Driver)
     */
    @Transactional
    public SwapTransaction completeMyTransaction(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        SwapTransaction transaction = swapTransactionRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        transaction.setStatus("Completed");
        transaction.setEndTime(LocalDateTime.now());

        return swapTransactionRepository.save(transaction);
    }

    /**
     * READ - Lấy tất cả transactions (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<SwapTransaction> getAllTransactions() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return swapTransactionRepository.findAll();
    }

    /**
     * UPDATE - Cập nhật transaction (Admin/Staff only)
     */
    @Transactional
    public SwapTransaction updateTransaction(Long id, SwapTransactionRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        SwapTransaction transaction = swapTransactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        // Update các field
        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new NotFoundException("Vehicle not found"));
            transaction.setVehicle(vehicle);
        }

        if (request.getStationId() != null) {
            Station station = stationRepository.findById(request.getStationId())
                    .orElseThrow(() -> new NotFoundException("Station not found"));
            transaction.setStation(station);
        }

        if (request.getStaffId() != null) {
            User staff = userRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new NotFoundException("Staff not found"));
            transaction.setStaff(staff);
        }

        if (request.getCost() != null) {
            transaction.setCost(request.getCost());
        }

        return swapTransactionRepository.save(transaction);
    }

    /**
     * UPDATE - Cập nhật status transaction (Admin/Staff only)
     */
    @Transactional
    public SwapTransaction updateTransactionStatus(Long id, String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        SwapTransaction transaction = swapTransactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        transaction.setStatus(status);

        if ("Completed".equals(status) && transaction.getEndTime() == null) {
            transaction.setEndTime(LocalDateTime.now());
        }

        return swapTransactionRepository.save(transaction);
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}