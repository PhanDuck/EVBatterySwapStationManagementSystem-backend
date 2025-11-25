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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    private final MoMoService moMoService;

    /**
     * Creates a new Vehicle với status UNPAID (chưa cọc)
     * Upload ảnh giấy đăng ký xe
     * CHỈ CHO PHÉP user có role DRIVER
     *
     * Flow:
     * 1. Tạo xe với status = UNPAID (cho phép hủy)
     * 2. Driver gọi API thanh toán cọc riêng
     * 3. Sau thanh toán -> status = PENDING
     * 4. Admin approve -> ACTIVE
     * 5. Admin reject -> depositStatus = REFUNDABLE, status = INACTIVE
     *
     * @return Vehicle đã tạo
     */
    @Transactional
    public Vehicle createVehicle(VehicleRequest vehicleRequest, MultipartFile registrationImageFile) {
        User currentUser = authenticationService.getCurrentUser();

        // Kiểm tra role: Chỉ DRIVER mới được tạo xe
        if (currentUser.getRole() != User.Role.DRIVER) {
            throw new AuthenticationException("Chỉ tài xế mới đăng ký xe!");
        }

        // Validate VIN unique - CHỈ kiểm tra xe ACTIVE, PENDING, UNPAID (bỏ qua INACTIVE)
        List<Vehicle.VehicleStatus> activeStatuses = List.of(Vehicle.VehicleStatus.ACTIVE, Vehicle.VehicleStatus.PENDING, Vehicle.VehicleStatus.UNPAID);
        if (vehicleRepository.existsByVinAndStatusIn(vehicleRequest.getVin(), activeStatuses)) {
            throw new IllegalArgumentException("VIN đã tồn tại!");
        }

        // Validate PlateNumber unique - CHỈ kiểm tra xe ACTIVE, PENDING, UNPAID (bỏ qua INACTIVE)
        if (vehicleRepository.existsByPlateNumberAndStatusIn(vehicleRequest.getPlateNumber(), activeStatuses)) {
            throw new IllegalArgumentException("Biển số xe đã tồn tại!");
        }

        // Validate battery type exists
        BatteryType batteryType = batteryTypeRepository.findById(vehicleRequest.getBatteryTypeId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));

        // Enforce max 2 ACTIVE vehicles per user (không đếm xe PENDING, UNPAID hoặc INACTIVE)
        long activeVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.ACTIVE).size();
        if (activeVehicles >= 2) {
            throw new IllegalStateException("Đã đủ 2 xe hoạt động!");
        }
        long pendingVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.PENDING).size();
        if (pendingVehicles >= 1) {
            throw new IllegalStateException("Có xe đang chờ duyệt!");
        }
        long unpaidVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.UNPAID).size();
        if (unpaidVehicles >= 1) {
            throw new IllegalStateException("Có xe chưa thanh toán cọc! Vui lòng thanh toán hoặc hủy xe trước.");
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

        // Set status = UNPAID - chưa thanh toán cọc (cho phép hủy)
        vehicle.setStatus(Vehicle.VehicleStatus.UNPAID);
        vehicle.setDepositStatus("PENDING");
        vehicle.setCreatedAt(LocalDateTime.now());

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return savedVehicle;
    }

    /**
     * Tạo payment URL để thanh toán cọc pin (400k VND)
     * CHỈ CHO PHÉP xe có status = UNPAID
     *
     * @param vehicleId ID của xe cần thanh toán cọc
     * @return Map chứa paymentUrl, orderId, amount
     */
    @Transactional
    public Map<String, Object> payDeposit(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));

        User currentUser = authenticationService.getCurrentUser();

        // Chỉ driver sở hữu xe mới được thanh toán cọc
        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Bạn không có quyền thanh toán cọc cho xe này!");
        }

        // Kiểm tra xe đang ở trạng thái UNPAID
        if (vehicle.getStatus() != Vehicle.VehicleStatus.UNPAID) {
            throw new IllegalStateException("Xe không ở trạng thái chưa cọc!");
        }

        // Tạo payment URL
        Map<String, String> paymentInfo = moMoService.createDepositPaymentUrl(vehicleId, null);

        // Trả về payment info
        Map<String, Object> result = new HashMap<>();
        result.put("vehicleId", vehicle.getId());
        result.put("paymentUrl", paymentInfo.get("payUrl"));
        result.put("orderId", paymentInfo.get("orderId"));
        result.put("amount", paymentInfo.get("amount"));
        result.put("message", "Vui lòng quét mã QR để thanh toán 400,000 VNĐ tiền cọc pin. Sau khi thanh toán thành công, xe sẽ tự động chuyển sang trạng thái chờ duyệt.");

        return result;
    }

    /**
     * Hủy xe khi đang ở trạng thái UNPAID (chưa thanh toán cọc)
     * CHỈ driver sở hữu xe mới được hủy
     *
     * @param vehicleId ID của xe cần hủy
     */
    @Transactional
    public void cancelUnpaidVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe"));

        User currentUser = authenticationService.getCurrentUser();

        // Chỉ driver sở hữu xe mới được hủy
        if (!vehicle.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Bạn không có quyền hủy xe này!");
        }

        // Chỉ hủy được xe UNPAID
        if (vehicle.getStatus() != Vehicle.VehicleStatus.UNPAID) {
            throw new IllegalStateException("Chỉ có thể hủy xe đang ở trạng thái chưa cọc!");
        }

        // Xóa xe khỏi DB
        vehicleRepository.delete(vehicle);
    }

    /**
     * READ - Lấy vehicles của tôi (Driver only) - lấy xe ACTIVE, PENDING và UNPAID
     */
    @Transactional(readOnly = true)
    public List<Vehicle> getMyVehicles() {
        User currentUser = authenticationService.getCurrentUser();
        List<Vehicle> activeVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.ACTIVE);
        List<Vehicle> pendingVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.PENDING);
        List<Vehicle> unpaidVehicles = vehicleRepository.findByDriverAndStatus(currentUser, Vehicle.VehicleStatus.UNPAID);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.addAll(activeVehicles);
        vehicles.addAll(pendingVehicles);
        vehicles.addAll(unpaidVehicles);

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
                throw new IllegalArgumentException("VIN phải đủ 17 ký tự!");
            }

            // Check duplicate
            if (vehicleRepository.existsByVin(vehicleRequest.getVin())) {
                throw new IllegalArgumentException("VIN đã tồn tại!");
            }
            existingVehicle.setVin(vehicleRequest.getVin());
        }

        // Kiểm tra trùng PlateNumber nếu thay đổi
        if (vehicleRequest.getPlateNumber() != null && !vehicleRequest.getPlateNumber().trim().isEmpty()
                && !vehicleRequest.getPlateNumber().equals(existingVehicle.getPlateNumber())) {

            // Validate format biển số (optional - có thể bỏ nếu không cần strict)
            String plateNumberPattern = "^[0-9]{2}[a-zA-Z]{1,2}[0-9]{5,6}(\\.[a-zA-Z]{1,2})?$";
            if (!vehicleRequest.getPlateNumber().matches(plateNumberPattern)) {
                throw new IllegalArgumentException("Biển số không đúng định dạng!");
            }

            // Check duplicate
            if (vehicleRepository.existsByPlateNumber(vehicleRequest.getPlateNumber())) {
                throw new IllegalArgumentException("Biển số xe đã tồn tại!");
            }
            existingVehicle.setPlateNumber(vehicleRequest.getPlateNumber());
        }

        // KHÔNG ĐỔI LOẠI PIN KHÁC KHI XE CÓ PIN
        if (vehicleRequest.getBatteryTypeId() != null) {
            if (existingVehicle.getCurrentBattery() != null
                    && !existingVehicle.getBatteryType().getId().equals(vehicleRequest.getBatteryTypeId())) {
                throw new IllegalStateException("Xe có pin, không đổi loại!");
            }

            BatteryType batteryType = batteryTypeRepository.findById(vehicleRequest.getBatteryTypeId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));
            existingVehicle.setBatteryType(batteryType);
        }

        // KHÔNG CHO ĐỔI DRIVER
        if (vehicleRequest.getDriverId() != null) {
            throw new IllegalStateException("Không được đổi chủ xe!");
        }

        return vehicleRepository.save(existingVehicle);
    }

    /**
     * DELETE - Soft delete xe (đổi status thành INACTIVE)
     * Admin/Staff only
     * Trả pin hiện tại về kho (nếu có)
     * Tự động hoàn cọc nếu đã thanh toán
     */
    @Transactional
    public Vehicle deleteVehicle(Long id, String refundNote) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối. Yêu cầu vai trò Admin/Staff.");
        }

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy xe với id: " + id));

        // Kiểm tra nếu vehicle đã bị xóa rồi
        if (vehicle.getStatus() == Vehicle.VehicleStatus.INACTIVE) {
            throw new IllegalStateException("Xe đã bị xóa!");
        }

        // KIỂM TRA: Xe đang có booking CONFIRMED (đang chờ đổi pin) thì không được xóa
        List<Booking> confirmedBookings = bookingRepository.findByVehicleAndStatusNotIn(
                vehicle,
                List.of(Booking.Status.CANCELLED, Booking.Status.COMPLETED)
        );

        if (!confirmedBookings.isEmpty()) {
            throw new IllegalStateException("Xe có booking, không xóa được!");
        }

        // CHO PHÉP XÓA XE CUỐI CÙNG - Driver có quyền rời khỏi hệ thống
        // Pin sẽ được trả về kho tự động

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

        // Xử lý tiền cọc: Nếu đã thanh toán cọc thì đánh dấu REFUNDED
        if ("PAID".equals(vehicle.getDepositStatus())) {
            vehicle.setDepositStatus("REFUNDED");
            String note = (refundNote != null ? refundNote : "Xe bị xóa - Đã hoàn tiền cọc")
                    + " - Ngày: " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            vehicle.setDepositRefundNote(note);
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
            throw new IllegalStateException("Xe không ở trạng thái chờ duyệt!");
        }

        // Kiểm tra driver có quá 2 xe ACTIVE chưa
        long activeVehicles = vehicleRepository.findByDriverAndStatus(vehicle.getDriver(), Vehicle.VehicleStatus.ACTIVE).size();
        if (activeVehicles >= 2) {
            throw new IllegalStateException("Tài xế đã đủ 2 xe!");
        }

        // Lấy pin từ kho
        Battery battery = batteryRepository.findById(request.getBatteryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy pin với id: " + request.getBatteryId()));

        // Kiểm tra pin phải ĐANG Ở TRONG KHO (currentStation = NULL và có trong StationInventory)
        if (battery.getStatus() != Battery.Status.AVAILABLE) {
            throw new IllegalStateException("Pin không sẵn sàng!");
        }

        // Pin trong KHO phải có currentStation = NULL (không thuộc trạm nào)
        if (battery.getCurrentStation() != null) {
            throw new IllegalStateException("Pin đang ở trạm, không gắn được!");
        }

        // Kiểm tra pin có trong StationInventory không (đảm bảo thực sự ở kho)
        boolean isInWarehouse = stationInventoryRepository.findByBattery(battery).isPresent();
        if (!isInWarehouse) {
            throw new IllegalStateException("Pin không có trong kho!");
        }

        // Kiểm tra loại pin có khớp với xe không
        if (!battery.getBatteryType().getId().equals(vehicle.getBatteryType().getId())) {
            throw new IllegalArgumentException("Loại pin không khớp!");
        }

        // Gắn pin vào xe
        vehicle.setCurrentBattery(battery);

        // LƯU GIÁ TRỊ PIN GỐC TRƯỚC KHI THAY ĐỔI BẤT KỲ GÌ
        BigDecimal originalChargeLevel = battery.getChargeLevel();

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

        // Gửi email thông báo cho tài xế (TRƯỚC KHI GIẢM PIN - email sẽ hiển thị pin cao)
        try {
            emailService.sendVehicleApprovedToDriver(savedVehicle);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không ảnh hưởng đến luồng chính
            System.err.println("Lỗi khi gửi email thông báo xe được phê duyệt: " + e.getMessage());
        }

        //SIMULATE: Giảm mức pin xuống random dưới 50% SAU KHI GỬI EMAIL
        // (Email hiển thị pin cao, nhưng xe thực tế có pin thấp để test)
        Random random = new Random();
        BigDecimal randomChargeLevel = BigDecimal.valueOf(10 + random.nextInt(40)); // Random 10-49%
        battery.setChargeLevel(randomChargeLevel);
        batteryRepository.save(battery);

        System.out.println(String.format("Pin ID %d được gắn vào xe - Email hiển thị: %.0f%%, Mức pin thực tế giảm xuống: %.0f%%",
                battery.getId(), originalChargeLevel.doubleValue(), randomChargeLevel.doubleValue()));

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
            throw new IllegalStateException("Xe không ở trạng thái chờ duyệt!");
        }

        // Chuyển status sang INACTIVE
        vehicle.setStatus(Vehicle.VehicleStatus.INACTIVE);
        vehicle.setDeletedAt(LocalDateTime.now());
        vehicle.setDeletedBy(currentUser);

        // Nếu đã thanh toán cọc (PAID), đánh dấu có thể hoàn tiền
        if ("PAID".equals(vehicle.getDepositStatus())) {
            vehicle.setDepositStatus("REFUNDABLE");
            String rejectionReason = (request != null && request.getRejectionReason() != null)
                    ? request.getRejectionReason()
                    : "Không có lý do cụ thể được cung cấp.";
            vehicle.setDepositRefundNote("Xe bị từ chối: " + rejectionReason);
        }

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