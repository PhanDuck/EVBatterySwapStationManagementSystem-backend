package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.StationRequest;
import com.evbs.BackEndEvBs.model.request.StationUpdateRequest;
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

        Station station = modelMapper.map(request, Station.class);
        return stationRepository.save(station);
    }

    /**
     * READ - Lấy tất cả stations (Public)
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

    @Transactional
    public Station updateStation(Long id, StationUpdateRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found"));

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
        stationRepository.delete(station);
    }

    /**
     * UPDATE - Cập nhật status station (Admin/Staff only)
     */
    @Transactional
    public Station updateStationStatus(Long id, String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found"));

        station.setStatus(status);
        return stationRepository.save(station);
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}
