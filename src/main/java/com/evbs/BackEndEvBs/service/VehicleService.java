package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.VehicleRejectRequest;
import com.evbs.BackEndEvBs.model.request.VehicleRequest;
import com.evbs.BackEndEvBs.model.request.VehicleApproveRequest;
import com.evbs.BackEndEvBs.model.request.VehicleUpdateRequest;
import com.evbs.BackEndEvBs.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final BatteryRepository batteryRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final EmailService emailService;

    @Autowired
    private final FileStorageService fileStorageService;

    /**
     * Creates a new Vehicle (status = PENDING, chờ admin duyệt)
     * Upload ảnh giấy đăng ký xe
     */
    @Transactional
    public Vehicle createVehicle(VehicleRequest vehicleRequest, MultipartFile registrationImageFile) {
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

        // Upload file ảnh giấy đăng ký
        String registrationImagePath = fileStorageService.uploadFile(registrationImageFile);

        // Create vehicle manually to avoid ModelMapper conflicts
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vehicleRequest.getVin());
        vehicle.setPlateNumber(vehicleRequest.getPlateNumber());
        vehicle.setModel(vehicleRequest.getModel());
        vehicle.setRegistrationImage(registrationImagePath);

        User currentUser = authenticationService.getCurrentUser();

        // Enforce max 2 ACTIVE vehicles per user (không đếm xe PENDING hoặc INACTIVE)
        long activeVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.ACTIVE).size();
        if (activeVehicles >= 2) {
            throw new AuthenticationException("Bạn chỉ có thể đăng ký tối đa 2 xe đang hoạt động.");
        }

        vehicle.setDriver(currentUser);
        vehicle.setBatteryType(batteryType);

        // Set status = PENDING - chờ admin duyệt
        vehicle.setStatus(Vehicle.VehicleStatus.PENDING);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // Gửi email thông báo cho admin
        try {
            List<User> adminList = userRepository.findByRole(User.Role.ADMIN);
            if (!adminList.isEmpty()) {
                emailService.sendVehicleRequestToAdmin(adminList, savedVehicle);
            }
        } catch (Exception e) {
            // Log error nhưng không throw exception để không ảnh hưởng đến việc tạo vehicle
            System.err.println("Lỗi khi gửi email thông báo cho admin: " + e.getMessage());
        }

        return savedVehicle;
    }

    /**
     * READ - Lấy vehicles của tôi (Driver only) - chỉ lấy xe ACTIVE
     */
    @Transactional(readOnly = true)
    public List<Vehicle> getMyVehicles() {
        User currentUser = authenticationService.getCurrentUser();
        List<Vehicle> vehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.ACTIVE);
        populateSwapCounts(vehicles);
        populateBatteryTypeNames(vehicles);
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
        populateBatteryTypeNames(vehicles);
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

    // Populate batteryTypeName for a list of vehicles
    private void populateBatteryTypeNames(List<Vehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) return;

        vehicles.forEach(v -> {
            if (v.getBatteryType() != null) {
                v.setBatteryTypeName(v.getBatteryType().getName());
            }
        });
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

    /**
     * APPROVE - Admin phê duyệt xe (chuyển từ PENDING sang ACTIVE) và gắn pin ban đầu
     */
    @Transactional
    public Vehicle approveVehicle(Long id, VehicleApproveRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối. Yêu cầu vai trò Admin/Staff.");
        }

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe với id: " + id));

        // Kiểm tra xe có đang ở trạng thái PENDING không
        if (vehicle.getStatus() != Vehicle.VehicleStatus.PENDING) {
            throw new AuthenticationException("Chỉ có thể phê duyệt xe đang ở trạng thái PENDING");
        }

        // Kiểm tra driver có quá 2 xe ACTIVE chưa
        long activeVehicles = vehicleRepository.findByDriverAndStatus(vehicle.getDriver(), Vehicle.VehicleStatus.ACTIVE).size();
        if (activeVehicles >= 2) {
            throw new AuthenticationException("Tài xế đã có 2 xe đang hoạt động. Không thể phê duyệt thêm.");
        }

        // Lấy pin từ kho
        Battery battery = batteryRepository.findById(request.getBatteryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin với id: " + request.getBatteryId()));

        // Kiểm tra pin có sẵn sàng không
        if (battery.getStatus() != Battery.Status.AVAILABLE) {
            throw new AuthenticationException("Pin không ở trạng thái AVAILABLE. Trạng thái hiện tại: " + battery.getStatus());
        }

        // Kiểm tra loại pin có khớp với xe không
        if (!battery.getBatteryType().getId().equals(vehicle.getBatteryType().getId())) {
            throw new AuthenticationException("Loại pin không khớp với xe. Xe yêu cầu loại pin: " + vehicle.getBatteryType().getName());
        }

        // Gắn pin vào xe
        vehicle.setCurrentBattery(battery);

        // Cập nhật trạng thái pin
        battery.setStatus(Battery.Status.IN_USE);
        batteryRepository.save(battery);

        // Chuyển status xe sang ACTIVE
        vehicle.setStatus(Vehicle.VehicleStatus.ACTIVE);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // Gửi email thông báo cho tài xế
        try {
            emailService.sendVehicleApprovedToDriver(savedVehicle);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không ảnh hưởng đến luồng chính
            System.err.println("Lỗi khi gửi email thông báo xe được phê duyệt: " + e.getMessage());
        }

        return savedVehicle;
    }

    /**
     * REJECT - Admin từ chối xe (chuyển từ PENDING sang INACTIVE)
     */
    @Transactional
    public Vehicle rejectVehicle(Long id, VehicleRejectRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối. Yêu cầu vai trò Admin/Staff.");
        }

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe với id: " + id));

        // Kiểm tra xe có đang ở trạng thái PENDING không
        if (vehicle.getStatus() != Vehicle.VehicleStatus.PENDING) {
            throw new AuthenticationException("Chỉ có thể từ chối xe đang ở trạng thái PENDING");
        }

        // Chuyển status sang INACTIVE
        vehicle.setStatus(Vehicle.VehicleStatus.INACTIVE);
        vehicle.setDeletedAt(LocalDateTime.now());
        vehicle.setDeletedBy(currentUser);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        // Gửi email thông báo cho tài xế
        try {
            String rejectionReason = (request != null && request.getRejectionReason() != null)
                    ? request.getRejectionReason()
                    : "Không có lý do cụ thể được cung cấp.";
            emailService.sendVehicleRejectedToDriver(savedVehicle, rejectionReason);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không ảnh hưởng đến luồng chính
            System.err.println("Lỗi khi gửi email thông báo xe bị từ chối: " + e.getMessage());
        }

        return savedVehicle;
    }
}