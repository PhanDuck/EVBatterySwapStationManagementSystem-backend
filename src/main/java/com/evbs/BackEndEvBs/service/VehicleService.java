package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.StationInventory;
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
import java.util.ArrayList;
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
    private final StationInventoryRepository stationInventoryRepository;

    @Autowired
    private final BookingRepository bookingRepository;

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
     * CHỈ CHO PHÉP user có role DRIVER
     */
    @Transactional
    public Vehicle createVehicle(VehicleRequest vehicleRequest, MultipartFile registrationImageFile) {
        User currentUser = authenticationService.getCurrentUser();
        
        // Kiểm tra role: Chỉ DRIVER mới được tạo xe
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Chỉ tài xế (DRIVER) mới có thể đăng ký xe!");
        }

        // Validate VIN unique - CHỈ kiểm tra xe ACTIVE hoặc PENDING (bỏ qua INACTIVE)
        List<Vehicle.VehicleStatus> activeStatuses = List.of(Vehicle.VehicleStatus.ACTIVE, Vehicle.VehicleStatus.PENDING);
        if (vehicleRepository.existsByVinAndStatusIn(vehicleRequest.getVin(), activeStatuses)) {
            throw new AuthenticationException("VIN đã tồn tại trong hệ thống!");
        }

        // Validate PlateNumber unique - CHỈ kiểm tra xe ACTIVE hoặc PENDING (bỏ qua INACTIVE)
        if (vehicleRepository.existsByPlateNumberAndStatusIn(vehicleRequest.getPlateNumber(), activeStatuses)) {
            throw new AuthenticationException("Biển số xe đã tồn tại trong hệ thống!");
        }

        // Validate battery type exists
        BatteryType batteryType = batteryTypeRepository.findById(vehicleRequest.getBatteryTypeId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));

        // Enforce max 2 ACTIVE vehicles per user (không đếm xe PENDING hoặc INACTIVE)
        long activeVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.ACTIVE).size();
        if (activeVehicles >= 2) {
            throw new AuthenticationException("Bạn chỉ có thể đăng ký tối đa 2 xe đang hoạt động.");
        }
        long pendingVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.PENDING).size();
        if (pendingVehicles >= 1) {
            throw new AuthenticationException("Bạn đang có xe đợi đăng kí vui lòng chờ.");
        }
        
        // Upload file ảnh giấy đăng ký
        String registrationImagePath = fileStorageService.uploadFile(registrationImageFile);

        // Create vehicle manually to avoid ModelMapper conflicts
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(vehicleRequest.getVin());
        vehicle.setPlateNumber(vehicleRequest.getPlateNumber());
        vehicle.setModel(vehicleRequest.getModel());
        vehicle.setRegistrationImage(registrationImagePath);
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
     * READ - Lấy vehicles của tôi (Driver only) - lấy xe ACTIVE và PENDING
     */
    @Transactional(readOnly = true)
    public List<Vehicle> getMyVehicles() {
        User currentUser = authenticationService.getCurrentUser();
        List<Vehicle> activeVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.ACTIVE);
        List<Vehicle> pendingVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.PENDING);
        
        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.addAll(activeVehicles);
        vehicles.addAll(pendingVehicles);
        
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
        populateDriverNames(vehicles);
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

    // Populate driverName for a list of vehicles
    private void populateDriverNames(List<Vehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) return;

        vehicles.forEach(v -> {
            if (v.getDriver() != null) {
                v.setDriverName(v.getDriver().getFullName());
            }
        });
    }

    /**
     * UPDATE - Cập nhật thông tin không quan trọng (Driver)
     */
    @Transactional
    public Vehicle updateMyVehicle(Long id, VehicleUpdateRequest vehicleRequest, MultipartFile registrationImageFile) {
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
     * Field nào null hoặc empty thì giữ nguyên giá trị cũ
     */
    @Transactional
    public Vehicle updateVehicle(Long id, VehicleUpdateRequest vehicleRequest, MultipartFile registrationImageFile) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối. Yêu cầu vai trò Admin/Staff.");
        }

        Vehicle existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));

        // Admin/Staff được update tất cả thông tin
        // Chỉ update nếu có giá trị mới (không null và không empty)
        if (vehicleRequest.getModel() != null && !vehicleRequest.getModel().trim().isEmpty()) {
            existingVehicle.setModel(vehicleRequest.getModel());
        }

        // Kiểm tra trùng VIN nếu thay đổi
        if (vehicleRequest.getVin() != null && !vehicleRequest.getVin().trim().isEmpty() 
            && !vehicleRequest.getVin().equals(existingVehicle.getVin())) {
            
            // Validate độ dài VIN
            if (vehicleRequest.getVin().length() != 17) {
                throw new AuthenticationException("VIN phải có chính xác 17 ký tự!");
            }
            
            // Check duplicate
            if (vehicleRepository.existsByVin(vehicleRequest.getVin())) {
                throw new AuthenticationException("VIN đã tồn tại!");
            }
            existingVehicle.setVin(vehicleRequest.getVin());
        }

        // Kiểm tra trùng PlateNumber nếu thay đổi
        if (vehicleRequest.getPlateNumber() != null && !vehicleRequest.getPlateNumber().trim().isEmpty()
            && !vehicleRequest.getPlateNumber().equals(existingVehicle.getPlateNumber())) {
            
            // Validate format biển số (optional - có thể bỏ nếu không cần strict)
            String plateNumberPattern = "^[0-9]{2}[a-zA-Z]{1,2}[0-9]{5,6}(\\.[a-zA-Z]{1,2})?$";
            if (!vehicleRequest.getPlateNumber().matches(plateNumberPattern)) {
                throw new AuthenticationException(
                    "Định dạng biển số xe máy Việt Nam không hợp lệ! Ví dụ hợp lệ: 29X112345, 51F11234, 30H112350"
                );
            }
            
            // Check duplicate
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
            User newDriver = userRepository.findById(vehicleRequest.getDriverId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy tài xế với ID: " + vehicleRequest.getDriverId()));
            
            // Kiểm tra user có vai trò DRIVER không
            if (newDriver.getRole() != User.Role.DRIVER) {
                throw new AuthenticationException("User này không phải là tài xế (DRIVER)");
            }
            
            // Kiểm tra driver mới không được có quá 2 xe (ACTIVE + PENDING)
            // CHỈ kiểm tra nếu xe hiện tại đang ACTIVE hoặc PENDING
            if (existingVehicle.getStatus() == Vehicle.VehicleStatus.ACTIVE 
                || existingVehicle.getStatus() == Vehicle.VehicleStatus.PENDING) {
                
                long newDriverActiveVehicles = vehicleRepository.findByDriverAndStatus(newDriver, Vehicle.VehicleStatus.ACTIVE).size();
                long newDriverPendingVehicles = vehicleRepository.findByDriverAndStatus(newDriver, Vehicle.VehicleStatus.PENDING).size();
                long totalVehicles = newDriverActiveVehicles + newDriverPendingVehicles;
                
                // Loại trừ xe hiện tại nếu cùng driver
                if (existingVehicle.getDriver().getId().equals(newDriver.getId())) {
                    totalVehicles--;
                }
                
                if (totalVehicles >= 2) {
                    throw new AuthenticationException(
                        "Tài xế " + newDriver.getFullName() + " đã có " + totalVehicles + 
                        " xe (ACTIVE + PENDING). Không thể gán thêm xe."
                    );
                }
            }
            
            existingVehicle.setDriver(newDriver);
        }

        return vehicleRepository.save(existingVehicle);
    }

    /**
     * DELETE - Soft delete xe (đổi status thành INACTIVE)
     * Admin/Staff only
     * Trả pin hiện tại về kho (nếu có)
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

        // KIỂM TRA: Xe đang có booking CONFIRMED (đang chờ đổi pin) thì không được xóa
        List<Booking> confirmedBookings = bookingRepository.findByVehicleAndStatusNotIn(
                vehicle,
                List.of(Booking.Status.CANCELLED, Booking.Status.COMPLETED)
        );

        if (!confirmedBookings.isEmpty()) {
            Booking activeBooking = confirmedBookings.get(0);
            throw new IllegalStateException(
                    String.format("Không thể xóa xe đang có lịch đặt chỗ hoạt động (ID: %d, Trạng thái: %s, Thời gian: %s). " +
                                    "Vui lòng hoàn tất hoặc hủy lịch đặt chỗ trước khi xóa xe.",
                            activeBooking.getId(),
                            activeBooking.getStatus(),
                            activeBooking.getBookingTime())
            );
        }

        // Xử lý pin hiện tại khi xóa xe
        Battery currentBattery = vehicle.getCurrentBattery();
        if (currentBattery != null) {
            // Pin từ xe bị xóa → chuyển về MAINTENANCE (cần kiểm tra/bảo dưỡng)
            // KHÔNG set AVAILABLE ngay vì cần admin kiểm tra trước
            currentBattery.setStatus(Battery.Status.MAINTENANCE);
            currentBattery.setCurrentStation(null);  // Pin chưa thuộc station nào
            batteryRepository.save(currentBattery);
            
            // Thêm pin vào StationInventory với status MAINTENANCE
            // Kiểm tra xem pin đã có trong StationInventory chưa
            StationInventory existingInventory = stationInventoryRepository.findByBattery(currentBattery).orElse(null);
            
            if (existingInventory != null) {
                // Cập nhật status
                existingInventory.setStatus(StationInventory.Status.MAINTENANCE);
                existingInventory.setLastUpdate(LocalDateTime.now());
                stationInventoryRepository.save(existingInventory);
            } else {
                // Tạo mới StationInventory
                StationInventory newInventory = new StationInventory();
                newInventory.setBattery(currentBattery);
                newInventory.setStatus(StationInventory.Status.MAINTENANCE);
                newInventory.setLastUpdate(LocalDateTime.now());
                stationInventoryRepository.save(newInventory);
            }
            
            // Gỡ pin khỏi xe
            vehicle.setCurrentBattery(null);
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

        // Kiểm tra pin phải ĐANG Ở TRONG KHO (currentStation = NULL và có trong StationInventory)
        if (battery.getStatus() != Battery.Status.AVAILABLE) {
            throw new AuthenticationException("Pin không ở trạng thái AVAILABLE. Trạng thái hiện tại: " + battery.getStatus());
        }
        
        // Pin trong KHO phải có currentStation = NULL (không thuộc trạm nào)
        if (battery.getCurrentStation() != null) {
            throw new AuthenticationException("Pin đang ở trạm (currentStation = " + battery.getCurrentStation().getId() + "). Chỉ được dùng pin từ kho (currentStation = NULL).");
        }
        
        // Kiểm tra pin có trong StationInventory không (đảm bảo thực sự ở kho)
        boolean isInWarehouse = stationInventoryRepository.findByBattery(battery).isPresent();
        if (!isInWarehouse) {
            throw new AuthenticationException("Pin không có trong kho (không có record trong StationInventory). Pin phải ở trong kho mới được gắn lên xe.");
        }

        // Kiểm tra loại pin có khớp với xe không
        if (!battery.getBatteryType().getId().equals(vehicle.getBatteryType().getId())) {
            throw new AuthenticationException("Loại pin không khớp với xe. Xe yêu cầu loại pin: " + vehicle.getBatteryType().getName());
        }

        // Gắn pin vào xe
        vehicle.setCurrentBattery(battery);

        // Cập nhật trạng thái pin: IN_USE và XÓA currentStation (pin đã rời kho)
        battery.setStatus(Battery.Status.IN_USE);
        battery.setCurrentStation(null);  // Pin không còn ở station nữa
        battery.setReservedForBooking(null);  // Xóa reservation nếu có
        batteryRepository.save(battery);

        // XÓA pin khỏi StationInventory (pin đã rời kho lên xe)
        stationInventoryRepository.findByBattery(battery).ifPresent(inventory -> {
            stationInventoryRepository.delete(inventory);
        });

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