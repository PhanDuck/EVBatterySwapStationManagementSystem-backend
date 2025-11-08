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
import com.evbs.BackEndEvBs.repository.SwapTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class VehicleService {

    @Autowired
    private final VehicleRepository vehicleRepository;

    @Autowired
    private final SwapTransactionRepository swapTransactionRepository;

    @Autowired
    private final BatteryTypeRepository batteryTypeRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    /**
     * Creates a new Vehicle
     */
    @Transactional
    public Vehicle createVehicle(VehicleRequest vehicleRequest) {
        // Validate VIN unique
        if (vehicleRepository.existsByVin(vehicleRequest.getVin())) {
            throw new AuthenticationException("VIN đã tồn tại!");
        }

        // Validate PlateNumber unique
        if (vehicleRepository.existsByPlateNumber(vehicleRequest.getPlateNumber())) {
            throw new AuthenticationException("Biển số xe đã tồn tại!");
        }

        // Validate battery type exists
        BatteryType batteryType = batteryTypeRepository.findById(vehicleRequest.getBatteryTypeId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));

        // Create vehicle manually to avoid ModelMapper conflicts
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vehicleRequest.getVin());
        vehicle.setPlateNumber(vehicleRequest.getPlateNumber());
        vehicle.setModel(vehicleRequest.getModel());
        User currentUser = authenticationService.getCurrentUser();

        // Enforce max 2 ACTIVE vehicles per user (không đếm xe đã xóa)
        long activeVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.ACTIVE).size();
        if (activeVehicles >= 2) {
            throw new AuthenticationException("Bạn chỉ có thể đăng ký tối đa 2 xe đang hoạt động.");
        }

        vehicle.setDriver(currentUser);
        vehicle.setBatteryType(batteryType);

        return vehicleRepository.save(vehicle);
    }

    /**
     * READ - Lấy vehicles của tôi (Driver only) - chỉ lấy xe ACTIVE
     */
    @Transactional(readOnly = true)
    public List<Vehicle> getMyVehicles() {
        User currentUser = authenticationService.getCurrentUser();
        List<Vehicle> vehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.ACTIVE);
        populateSwapCounts(vehicles);
        return vehicles;
    }

    /**
     * READ - Lấy tất cả xe (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối. Yêu cầu vai trò Admin/Staff.");
        }
        List<Vehicle> vehicles = vehicleRepository.findAll();
        populateSwapCounts(vehicles);
        return vehicles;
    }

    // Populate swapCount for a list of vehicles using a single grouped query
    private void populateSwapCounts(List<Vehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) return;

        List<Long> ids = vehicles.stream()
                .map(Vehicle::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            vehicles.forEach(v -> v.setSwapCount(0L));
            return;
        }

        List<Object[]> counts = swapTransactionRepository.countByVehicleIds(ids);
        Map<Long, Long> countMap = new HashMap<>();
        for (Object[] row : counts) {
            Long vehicleId = (Long) row[0];
            Long cnt = (Long) row[1];
            countMap.put(vehicleId, cnt);
        }

        vehicles.forEach(v -> v.setSwapCount(countMap.getOrDefault(v.getId(), 0L)));
    }

    /**
     * UPDATE - Cập nhật thông tin không quan trọng (Driver)
     */
    @Transactional
    public Vehicle updateMyVehicle(Long id, VehicleUpdateRequest vehicleRequest) {
        User currentUser = authenticationService.getCurrentUser();
        Vehicle existingVehicle = vehicleRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe hoặc truy cập bị từ chối"));

        // Driver chỉ được update model, không được thay đổi VIN, PlateNumber
        if (vehicleRequest.getModel() != null && !vehicleRequest.getModel().trim().isEmpty()) {
            existingVehicle.setModel(vehicleRequest.getModel());
        }

        // Driver có thể update battery type
        if (vehicleRequest.getBatteryTypeId() != null) {
            BatteryType batteryType = batteryTypeRepository.findById(vehicleRequest.getBatteryTypeId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));
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
            throw new AuthenticationException("Truy cập bị từ chối. Yêu cầu vai trò Admin/Staff.");
        }

        Vehicle existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));

        // Admin/Staff được update tất cả thông tin
        if (vehicleRequest.getModel() != null) {
            existingVehicle.setModel(vehicleRequest.getModel());
        }

        // Kiểm tra trùng VIN nếu thay đổi
        if (vehicleRequest.getVin() != null && !vehicleRequest.getVin().equals(existingVehicle.getVin())) {
            if (vehicleRepository.existsByVin(vehicleRequest.getVin())) {
                throw new AuthenticationException("VIN đã tồn tại!");
            }
            existingVehicle.setVin(vehicleRequest.getVin());
        }

        // Kiểm tra trùng PlateNumber nếu thay đổi
        if (vehicleRequest.getPlateNumber() != null && !vehicleRequest.getPlateNumber().equals(existingVehicle.getPlateNumber())) {
            if (vehicleRepository.existsByPlateNumber(vehicleRequest.getPlateNumber())) {
                throw new AuthenticationException("Biển số xe đã tồn tại!");
            }
            existingVehicle.setPlateNumber(vehicleRequest.getPlateNumber());
        }

        // Update battery type nếu có
        if (vehicleRequest.getBatteryTypeId() != null) {
            BatteryType batteryType = batteryTypeRepository.findById(vehicleRequest.getBatteryTypeId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));
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
     * DELETE - Soft delete xe (đổi status thành INACTIVE)
     * Admin/Staff only
     */
    @Transactional
    public Vehicle deleteVehicle(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối. Yêu cầu vai trò Admin/Staff.");
        }

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe với id: " + id));

        // Kiểm tra nếu vehicle đã bị xóa rồi
        if (vehicle.getStatus() == Vehicle.VehicleStatus.INACTIVE) {
            throw new AuthenticationException("Xe đã bị xóa trước đó");
        }

        // Soft delete: chỉ đổi status
        vehicle.setStatus(Vehicle.VehicleStatus.INACTIVE);
        vehicle.setDeletedAt(LocalDateTime.now());
        vehicle.setDeletedBy(currentUser);

        return vehicleRepository.save(vehicle);
    }

    /**
     * Helper method kiểm tra role
     */
    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}