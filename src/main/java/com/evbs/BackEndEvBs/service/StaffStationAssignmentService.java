package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.StaffStationAssignment;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.StaffStationAssignmentRequest;
import com.evbs.BackEndEvBs.repository.StaffStationAssignmentRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.evbs.BackEndEvBs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffStationAssignmentService {

    @Autowired
    private final StaffStationAssignmentRepository assignmentRepository;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    // Giới hạn số trạm tối đa 1 staff có thể quản lý
    private static final int MAX_STATIONS_PER_STAFF = 5;

    // Giới hạn số staff tối đa 1 trạm có thể có
    private static final int MAX_STAFF_PER_STATION = 2;

    /**
     * CREATE - Admin assign station cho staff
     */
    @Transactional
    public StaffStationAssignment assignStaffToStation(StaffStationAssignmentRequest request) {
        User currentAdmin = authenticationService.getCurrentUser();

        // ✅ Chỉ Admin mới có quyền assign
        if (currentAdmin.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Only admins can assign staff to stations.");
        }

        // Validate staff exists và có role = STAFF
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        if (staff.getRole() != User.Role.STAFF) {
            throw new AuthenticationException("User is not a staff member. Only STAFF role can be assigned to stations.");
        }

        // Validate station exists
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Station not found"));

        // Kiểm tra đã assign chưa
        if (assignmentRepository.existsByStaffAndStation(staff, station)) {
            throw new AuthenticationException("Staff is already assigned to this station");
        }

        // Kiểm tra số lượng stations của staff (max 5)
        long currentCount = assignmentRepository.countByStaff(staff);
        if (currentCount >= MAX_STATIONS_PER_STAFF) {
            throw new AuthenticationException(
                    String.format("Staff cannot manage more than %d stations. Current: %d",
                            MAX_STATIONS_PER_STAFF, currentCount)
            );
        }

        // ✅ Kiểm tra số lượng staff của station (max 2)
        long staffCountAtStation = assignmentRepository.countByStation(station);
        if (staffCountAtStation >= MAX_STAFF_PER_STATION) {
            throw new AuthenticationException(
                    String.format("Station cannot have more than %d staff members. Current: %d",
                            MAX_STAFF_PER_STATION, staffCountAtStation)
            );
        }

        // Tạo assignment
        StaffStationAssignment assignment = new StaffStationAssignment();
        assignment.setStaff(staff);
        assignment.setStation(station);
        // AssignedAt sẽ tự động = now() trong entity

        return assignmentRepository.save(assignment);
    }

    /**
     * DELETE - Admin tước quyền station của staff
     */
    @Transactional
    public void unassignStaffFromStation(Long staffId, Long stationId) {
        User currentAdmin = authenticationService.getCurrentUser();

        // ✅ Chỉ Admin mới có quyền unassign
        if (currentAdmin.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied. Only admins can unassign staff from stations.");
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found"));

        StaffStationAssignment assignment = assignmentRepository.findByStaffAndStation(staff, station)
                .orElseThrow(() -> new NotFoundException("Assignment not found. Staff is not assigned to this station."));

        assignmentRepository.delete(assignment);
    }

    /**
     * READ - Lấy tất cả stations của 1 staff
     */
    @Transactional(readOnly = true)
    public List<Station> getStationsByStaff(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        return assignmentRepository.findStationsByStaff(staff);
    }

    /**
     * READ - Lấy tất cả staff của 1 station (Admin only)
     */
    @Transactional(readOnly = true)
    public List<User> getStaffByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied");
        }

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Station not found"));

        return assignmentRepository.findStaffByStation(station);
    }

    /**
     * READ - Lấy tất cả assignments (Admin only)
     */
    @Transactional(readOnly = true)
    public List<StaffStationAssignment> getAllAssignments() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied");
        }

        return assignmentRepository.findAll();
    }

    /**
     * READ - Lấy các stations mà staff hiện tại đang quản lý
     */
    @Transactional(readOnly = true)
    public List<Station> getMyAssignedStations() {
        User currentStaff = authenticationService.getCurrentUser();

        if (currentStaff.getRole() != User.Role.STAFF) {
            throw new AuthenticationException("Only staff can view their assigned stations");
        }

        return assignmentRepository.findStationsByStaff(currentStaff);
    }

    /**
     * READ - Lấy tất cả assignments của 1 staff
     */
    @Transactional(readOnly = true)
    public List<StaffStationAssignment> getAssignmentsByStaff(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff not found"));

        return assignmentRepository.findByStaff(staff);
    }

    /**
     * HELPER - Kiểm tra staff có quyền quản lý station không
     */
    public boolean canStaffManageStation(User staff, Station station) {
        // Admin quản lý tất cả
        if (staff.getRole() == User.Role.ADMIN) {
            return true;
        }

        // Staff chỉ quản lý được stations được assign
        if (staff.getRole() == User.Role.STAFF) {
            return assignmentRepository.existsByStaffAndStation(staff, station);
        }

        return false;
    }

    /**
     * VALIDATION - Kiểm tra current user có quyền truy cập station không
     * Throws exception nếu không có quyền
     */
    public void validateStationAccess(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        
        // Admin có quyền truy cập tất cả
        if (currentUser.getRole() == User.Role.ADMIN) {
            return;
        }
        
        // Staff chỉ được truy cập stations được assign
        if (currentUser.getRole() == User.Role.STAFF) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new NotFoundException("Station not found with ID: " + stationId));
            
            boolean hasAccess = assignmentRepository.existsByStaffAndStation(currentUser, station);
            
            if (!hasAccess) {
                throw new AuthenticationException(
                    "Access denied. You are not assigned to manage this station (ID: " + stationId + ")"
                );
            }
            return;
        }
        
        throw new AuthenticationException("Access denied");
    }
}
