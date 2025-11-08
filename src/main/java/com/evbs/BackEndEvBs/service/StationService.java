package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.BatteryType;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.StationRequest;
import com.evbs.BackEndEvBs.model.request.StationUpdateRequest;
import com.evbs.BackEndEvBs.repository.BatteryTypeRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.evbs.BackEndEvBs.repository.StaffStationAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StationService {

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final BatteryTypeRepository batteryTypeRepository;

    @Autowired
    private final StaffStationAssignmentRepository staffStationAssignmentRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final BatteryHealthService batteryHealthService;

    /**
     * CREATE - Tạo station mới (Admin/Staff only)
     */
    @Transactional
    public Station createStation(StationRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối");
        }

        // Kiểm tra trùng tên station
        if (stationRepository.existsByName(request.getName())) {
            throw new AuthenticationException("Tên trạm đã tồn tại");
        }

        // Validate battery type
        BatteryType batteryType = batteryTypeRepository.findById(request.getBatteryTypeId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));

        //  Tạo station thủ công thay vì dùng ModelMapper (tránh conflict)
        Station station = new Station();
        station.setName(request.getName());
        station.setLocation(request.getLocation());
        station.setCapacity(request.getCapacity());
        station.setContactInfo(request.getContactInfo());
        station.setCity(request.getCity());
        station.setDistrict(request.getDistrict());
        station.setLatitude(request.getLatitude());
        station.setLongitude(request.getLongitude());
        station.setBatteryType(batteryType);
        // status = ACTIVE (default)

        return stationRepository.save(station);
    }

    /**
     * READ - Lấy tất cả stations (PUBLIC - không cần đăng nhập)
     * Ai cũng xem được tất cả stations (kể cả đang bảo trì, inactive, v.v.)
     */
    @Transactional(readOnly = true)
    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    /**
     * INTERNAL - Lấy station theo ID (dùng nội bộ)
     */
    @Transactional(readOnly = true)
    Station getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));
    }

    /**
     * READ - Lấy stations theo battery type (Public)
     */
    @Transactional(readOnly = true)
    public List<Station> getStationsByBatteryType(Long batteryTypeId) {
        BatteryType batteryType = batteryTypeRepository.findById(batteryTypeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));
        return stationRepository.findByBatteryType(batteryType);
    }

    /**
     * READ - Lấy stations tương thích với vehicle (Public)
     */
    @Transactional(readOnly = true)
    public List<Station> getCompatibleStations(Long vehicleId) {
        // Cần có VehicleRepository để lấy vehicle, nhưng hiện tại chưa có
        // Có thể implement sau khi có VehicleService
        // Tạm thời trả về tất cả stations
        return stationRepository.findAll();
    }

    @Transactional
    public Station updateStation(Long id, StationUpdateRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));

        //  Staff chỉ update được stations được assign
        if (currentUser.getRole() == User.Role.STAFF) {
            if (!staffStationAssignmentRepository.existsByStaffAndStation(currentUser, station)) {
                throw new AuthenticationException("Bạn không được phân công quản lý trạm này");
            }
        }

        // Kiểm tra trùng tên station (nếu thay đổi tên)
        if (request.getName() != null && !station.getName().equals(request.getName()) &&
                stationRepository.existsByName(request.getName())) {
            throw new AuthenticationException("Tên trạm đã tồn tại");
        }

        // Chỉ update những field không null (giữ lại giá trị cũ nếu không nhập)
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            station.setName(request.getName());
        }
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            station.setLocation(request.getLocation());
        }
        if (request.getCity() != null && !request.getCity().trim().isEmpty()) {
            station.setCity(request.getCity());
        }
        if (request.getDistrict() != null && !request.getDistrict().trim().isEmpty()) {
            station.setDistrict(request.getDistrict());
        }
        if (request.getCapacity() != null) {
            station.setCapacity(request.getCapacity());
        }
        if (request.getContactInfo() != null && !request.getContactInfo().trim().isEmpty()) {
            station.setContactInfo(request.getContactInfo());
        }
        if (request.getLatitude() != null) {
            station.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            station.setLongitude(request.getLongitude());
        }
        if (request.getBatteryTypeId() != null) {
            BatteryType batteryType = batteryTypeRepository.findById(request.getBatteryTypeId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy loại pin"));
            station.setBatteryType(batteryType);
        }

        // Cập nhật status nếu được cung cấp
        if (request.getStatus() != null) {
            // Staff chỉ update status cho stations được assign
            if (currentUser.getRole() == User.Role.STAFF) {
                if (!staffStationAssignmentRepository.existsByStaffAndStation(currentUser, station)) {
                    throw new AuthenticationException("Bạn không được phân công quản lý trạm này");
                }
            }
            station.setStatus(request.getStatus());
        }

        return stationRepository.save(station);
    }

    /**
     * DELETE - Xóa station (Admin only)
     */
    @Transactional
    public void deleteStation(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Truy cập bị từ chối");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));
        station.setStatus(Station.Status.INACTIVE);
        stationRepository.save(station);

    }

    /**
     * Lấy pin cần bảo trì tại trạm cụ thể
     * Staff chỉ xem được pins của stations mình quản lý
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getBatteriesNeedingMaintenanceAtStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        
        // Validate station exists first
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));
        
        // Validate station access for staff
        if ("STAFF".equals(currentUser.getRole())) {
            boolean hasAccess = staffStationAssignmentRepository
                    .existsByStaffAndStation(currentUser, station);
            if (!hasAccess) {
                throw new AuthenticationException("Bạn không có quyền truy cập trạm này");
            }
        }

        // Lấy tất cả pin có SOH < 70% (từ BatteryHealthService)
        List<com.evbs.BackEndEvBs.entity.Battery> allBatteriesNeedMaintenance = batteryHealthService.getBatteriesNeedingMaintenance();
        
        // Filter chỉ lấy pin Ở TRẠM NÀY (currentStation = stationId)
        List<com.evbs.BackEndEvBs.entity.Battery> batteriesAtStation = allBatteriesNeedMaintenance.stream()
                .filter(b -> b.getCurrentStation() != null && b.getCurrentStation().getId().equals(stationId))
                .collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("stationId", stationId);
        response.put("stationName", station.getName());
        response.put("location", "AT_STATION");
        response.put("total", batteriesAtStation.size());
        response.put("batteries", batteriesAtStation);
        response.put("message", batteriesAtStation.isEmpty() 
            ? "Trạm này không có pin nào cần bảo trì" 
            : "Trạm này có " + batteriesAtStation.size() + " pin cần bảo trì");
        
        return response;
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}