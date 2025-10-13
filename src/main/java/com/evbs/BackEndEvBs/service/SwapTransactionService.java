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

/**
 * Service xử lý các giao dịch hoán đổi pin
 * 
 * LOGIC QUAN TRỌNG:
 * - Khi swap transaction được COMPLETED (hoàn thành):
 *   + Pin swapOut (được đem ra khỏi trạm): currentStation = null, status = IN_USE
 *   + Pin swapIn (được đem vào trạm): currentStation = station, status = AVAILABLE
 * 
 * - Điều này đảm bảo:
 *   + Pin được lấy ra sẽ không còn tính vào capacity của trạm
 *   + Trạm sẽ có chỗ trống để nhận pin mới
 *   + Pin đang được sử dụng bên ngoài có thể được track
 */
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

        SwapTransaction savedTransaction = swapTransactionRepository.save(transaction);

        // Nếu transaction được tạo với status COMPLETED, xử lý logic pin
        if (SwapTransaction.Status.COMPLETED.equals(savedTransaction.getStatus())) {
            handleBatterySwap(savedTransaction);
        }

        return savedTransaction;
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

        transaction.setStatus(SwapTransaction.Status.COMPLETED);
        transaction.setEndTime(LocalDateTime.now());

        // Logic: Khi hoàn thành swap, pin được đem ra sẽ không còn ở trạm
        handleBatterySwap(transaction);

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
    public SwapTransaction updateTransactionStatus(Long id, SwapTransaction.Status status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        SwapTransaction transaction = swapTransactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        transaction.setStatus(status);

        if (SwapTransaction.Status.COMPLETED.equals(status) && transaction.getEndTime() == null) {
            transaction.setEndTime(LocalDateTime.now());
        }

        // Logic: Khi hoàn thành swap, pin được đem ra sẽ không còn ở trạm
        if (SwapTransaction.Status.COMPLETED.equals(status)) {
            handleBatterySwap(transaction);
        }

        return swapTransactionRepository.save(transaction);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Xử lý logic pin khi swap được hoàn thành
     * - Pin swapOut (được đem ra): currentStation = null, status = IN_USE
     * - Pin swapIn (được đem vào): currentStation = station của transaction, status = AVAILABLE
     */
    private void handleBatterySwap(SwapTransaction transaction) {
        // Xử lý pin được đem ra khỏi trạm
        if (transaction.getSwapOutBattery() != null) {
            Battery swapOutBattery = transaction.getSwapOutBattery();
            swapOutBattery.setCurrentStation(null); // Không còn ở trạm nào
            swapOutBattery.setStatus(Battery.Status.IN_USE); // Đang được sử dụng
            batteryRepository.save(swapOutBattery);
        }

        // Xử lý pin được đem vào trạm (nếu có)
        if (transaction.getSwapInBattery() != null) {
            Battery swapInBattery = transaction.getSwapInBattery();
            swapInBattery.setCurrentStation(transaction.getStation()); // Gán vào trạm
            swapInBattery.setStatus(Battery.Status.AVAILABLE); // Có sẵn để sử dụng
            batteryRepository.save(swapInBattery);
        }
    }

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}