package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.StationRequest;
import com.evbs.BackEndEvBs.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationService {

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    // ==================== PUBLIC READ OPERATIONS ====================

    /**
     * READ - Lấy tất cả stations (Public - Anyone can view)
     */
    @Transactional(readOnly = true)
    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    /**
     * READ - Lấy station theo ID (Public - Anyone can view)
     */
    @Transactional(readOnly = true)
    public Station getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + id));
    }

    /**
     * READ - Lấy active stations (Public)
     */
    @Transactional(readOnly = true)
    public List<Station> getActiveStations() {
        return stationRepository.findByStatusOrderByNameAsc("Active");
    }

    /**
     * READ - Tìm stations theo name (Public)
     */
    @Transactional(readOnly = true)
    public List<Station> searchStationsByName(String name) {
        return stationRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * READ - Tìm stations theo location (Public)
     */
    @Transactional(readOnly = true)
    public List<Station> searchStationsByLocation(String location) {
        return stationRepository.findByLocationContainingIgnoreCase(location);
    }

    /**
     * READ - Lấy stations có available batteries (Public - Driver quan tâm)
     */
    @Transactional(readOnly = true)
    public List<Station> getStationsWithAvailableBatteries() {
        return stationRepository.findStationsWithAvailableBatteries();
    }

    /**
     * READ - Lấy stations theo capacity range (Public)
     */
    @Transactional(readOnly = true)
    public List<Station> getStationsByCapacityRange(Integer minCapacity, Integer maxCapacity) {
        return stationRepository.findByCapacityBetween(minCapacity, maxCapacity);
    }

    /**
     * READ - Lấy stations theo status (Public)
     */
    @Transactional(readOnly = true)
    public List<Station> getStationsByStatus(String status) {
        return stationRepository.findByStatus(status);
    }

    // ==================== ADMIN/STAFF OPERATIONS ====================

    /**
     * CREATE - Tạo station mới (Admin/Staff only)
     */
    @Transactional
    public Station createStation(StationRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        // Kiểm tra trùng tên station
        if (stationRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Station with name '" + request.getName() + "' already exists");
        }

        Station station = modelMapper.map(request, Station.class);
        return stationRepository.save(station);
    }

    /**
     * UPDATE - Cập nhật station (Admin/Staff only)
     */
    @Transactional
    public Station updateStation(Long id, StationRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Station existingStation = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + id));

        // Kiểm tra trùng tên station (nếu thay đổi tên)
        if (!existingStation.getName().equals(request.getName()) &&
                stationRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Station with name '" + request.getName() + "' already exists");
        }

        // Sử dụng ModelMapper để cập nhật các field
        modelMapper.map(request, existingStation);

        return stationRepository.save(existingStation);
    }

    /**
     * UPDATE - Chỉ cập nhật status (Admin/Staff only)
     */
    @Transactional
    public Station updateStationStatus(Long id, String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + id));

        station.setStatus(status);
        return stationRepository.save(station);
    }

    /**
     * UPDATE - Chỉ cập nhật capacity (Admin/Staff only)
     */
    @Transactional
    public Station updateStationCapacity(Long id, Integer capacity) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + id));

        station.setCapacity(capacity);
        return stationRepository.save(station);
    }

    /**
     * DELETE - Xóa station (Admin only)
     */
    @Transactional
    public void deleteStation(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Admin role required.");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + id));

        // Kiểm tra xem station có đang được sử dụng không (có batteries, bookings, etc.)
        if (!station.getBatteries().isEmpty()) {
            throw new IllegalStateException("Cannot delete station with associated batteries. Remove batteries first.");
        }

        if (!station.getBookings().isEmpty()) {
            throw new IllegalStateException("Cannot delete station with associated bookings. Cancel bookings first.");
        }

        if (!station.getSwapTransactions().isEmpty()) {
            throw new IllegalStateException("Cannot delete station with associated transactions.");
        }

        stationRepository.delete(station);
    }

    /**
     * SOFT DELETE - Deactivate station (Admin/Staff only)
     */
    @Transactional
    public Station deactivateStation(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found with id: " + id));

        station.setStatus("Inactive");
        return stationRepository.save(station);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Đếm số stations theo status (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public long countStationsByStatus(String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }
        return stationRepository.countByStatus(status);
    }

    /**
     * Kiểm tra station có tồn tại không
     */
    @Transactional(readOnly = true)
    public boolean stationExists(Long id) {
        return stationRepository.existsById(id);
    }

    /**
     * Lấy station statistics (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public StationStatistics getStationStatistics() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied. Admin/Staff role required.");
        }

        long totalStations = stationRepository.count();
        long activeStations = stationRepository.countByStatus("Active");
        long inactiveStations = stationRepository.countByStatus("Inactive");
        long maintenanceStations = stationRepository.countByStatus("Maintenance");

        return new StationStatistics(totalStations, activeStations, inactiveStations, maintenanceStations);
    }

    /**
     * Helper method kiểm tra role
     */
    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }

    /**
     * Inner class for statistics
     */
    public static class StationStatistics {
        private final long totalStations;
        private final long activeStations;
        private final long inactiveStations;
        private final long maintenanceStations;

        public StationStatistics(long totalStations, long activeStations, long inactiveStations, long maintenanceStations) {
            this.totalStations = totalStations;
            this.activeStations = activeStations;
            this.inactiveStations = inactiveStations;
            this.maintenanceStations = maintenanceStations;
        }

        // Getters
        public long getTotalStations() { return totalStations; }
        public long getActiveStations() { return activeStations; }
        public long getInactiveStations() { return inactiveStations; }
        public long getMaintenanceStations() { return maintenanceStations; }
    }
}