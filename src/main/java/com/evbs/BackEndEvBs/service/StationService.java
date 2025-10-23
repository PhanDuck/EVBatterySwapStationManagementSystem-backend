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

    /**
     * CREATE - Tạo station mới (Admin/Staff only)
     */
    @Transactional
    public Station createStation(StationRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        // Kiểm tra trùng tên station
        if (stationRepository.existsByName(request.getName())) {
            throw new AuthenticationException("Station name already exists");
        }

        // Validate battery type
        BatteryType batteryType = batteryTypeRepository.findById(request.getBatteryTypeId())
                .orElseThrow(() -> new NotFoundException("Battery type not found"));

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
     * READ - Lấy station theo ID (Public)
     */
    @Transactional(readOnly = true)
    public Station getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found"));
    }

    /**
     * READ - Lấy stations theo battery type (Public)
     */
    @Transactional(readOnly = true)
    public List<Station> getStationsByBatteryType(Long batteryTypeId) {
        BatteryType batteryType = batteryTypeRepository.findById(batteryTypeId)
                .orElseThrow(() -> new NotFoundException("Battery type not found"));
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
            throw new AuthenticationException("Access denied");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found"));

        //  Staff chỉ update được stations được assign
        if (currentUser.getRole() == User.Role.STAFF) {
            if (!staffStationAssignmentRepository.existsByStaffAndStation(currentUser, station)) {
                throw new AuthenticationException("You are not assigned to manage this station");
            }
        }

        // Kiểm tra trùng tên station (nếu thay đổi tên)
        if (request.getName() != null && !station.getName().equals(request.getName()) &&
                stationRepository.existsByName(request.getName())) {
            throw new AuthenticationException("Station name already exists");
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
                    .orElseThrow(() -> new NotFoundException("Battery type not found"));
            station.setBatteryType(batteryType);
        }

        // Cập nhật status nếu được cung cấp
        if (request.getStatus() != null) {
            // Staff chỉ update status cho stations được assign
            if (currentUser.getRole() == User.Role.STAFF) {
                if (!staffStationAssignmentRepository.existsByStaffAndStation(currentUser, station)) {
                    throw new AuthenticationException("You are not assigned to manage this station");
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
            throw new AuthenticationException("Access denied");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found"));
        station.setStatus(Station.Status.INACTIVE);
        stationRepository.save(station);
        
    }

    /**
     * UPDATE - Cập nhật status station (Admin/Staff only)
     */
    @Transactional
    public Station updateStationStatus(Long id, Station.Status status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found"));

        //  Staff chỉ update được stations được assign
        if (currentUser.getRole() == User.Role.STAFF) {
            if (!staffStationAssignmentRepository.existsByStaffAndStation(currentUser, station)) {
                throw new AuthenticationException("You are not assigned to manage this station");
            }
        }

        station.setStatus(status);
        return stationRepository.save(station);
    }
    /**
     * Lấy tất cả pin cần bảo trì tại các trạm
     * Admin: Xem tất cả pins needs-maintenance của tất cả trạm
     * Staff: Chỉ xem pins needs-maintenance của trạm mình quản lý
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllBatteriesNeedingMaintenanceAtStations() {
        // Logic này sẽ được implement trong BatteryHealthService
        // Tạm thời trả về empty result
        Map<String, Object> response = new HashMap<>();
        response.put("location", "AT_STATIONS");
        response.put("total", 0);
        response.put("batteries", List.of());
        response.put("message", "Không có pin nào cần bảo trì tại các trạm");
        return response;
    }

    /**
     * Lấy pin cần bảo trì tại trạm cụ thể
     * Staff chỉ xem được pins của stations mình quản lý
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getBatteriesNeedingMaintenanceAtStation(Long stationId) {
        // Validate station exists
        Station station = getStationById(stationId);

        // Logic này sẽ được implement trong BatteryHealthService
        // Tạm thời trả về empty result
        Map<String, Object> response = new HashMap<>();
        response.put("stationId", stationId);
        response.put("stationName", station.getName());
        response.put("location", "AT_STATION");
        response.put("total", 0);
        response.put("batteries", List.of());
        response.put("message", "Trạm này không có pin nào cần bảo trì");
        return response;
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}