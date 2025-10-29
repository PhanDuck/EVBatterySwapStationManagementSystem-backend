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

    // Giới hạn số trạm tối đa mà 1 nhân viên có thể quản lý
    private static final int MAX_STATIONS_PER_STAFF = 5;

    // Giới hạn số nhân viên tối đa mà 1 trạm có thể có
    private static final int MAX_STAFF_PER_STATION = 3;

    /**
     * CREATE - Quản trị viên (Admin) gán trạm cho nhân viên
     */
    @Transactional
    public StaffStationAssignment assignStaffToStation(StaffStationAssignmentRequest request) {
        User currentAdmin = authenticationService.getCurrentUser();

        //  Chỉ Admin mới có quyền gán nhân viên vào trạm
        if (currentAdmin.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Từ chối truy cập. Chỉ quản trị viên mới có quyền gán nhân viên vào trạm.");
        }

        // Kiểm tra nhân viên có tồn tại và có vai trò STAFF không
        User staff = userRepository.findById(request.getStaffId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên."));

        if (staff.getRole() != User.Role.STAFF) {
            throw new AuthenticationException("Người dùng này không phải là nhân viên. Chỉ tài khoản có vai trò STAFF mới được gán vào trạm.");
        }

        // Kiểm tra trạm có tồn tại không
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm."));

        // Kiểm tra xem nhân viên đã được gán vào trạm này chưa
        if (assignmentRepository.existsByStaffAndStation(staff, station)) {
            throw new AuthenticationException("Nhân viên này đã được gán vào trạm này rồi.");
        }

        // Kiểm tra số lượng trạm mà nhân viên đang quản lý (tối đa 5)
        long currentCount = assignmentRepository.countByStaff(staff);
        if (currentCount >= MAX_STATIONS_PER_STAFF) {
            throw new AuthenticationException(
                    String.format("Nhân viên không thể quản lý quá %d trạm. Hiện tại: %d",
                            MAX_STATIONS_PER_STAFF, currentCount)
            );
        }

        // Kiểm tra số lượng nhân viên tại trạm (tối đa 3)
        long staffCountAtStation = assignmentRepository.countByStation(station);
        if (staffCountAtStation >= MAX_STAFF_PER_STATION) {
            throw new AuthenticationException(
                    String.format("Trạm không thể có hơn %d nhân viên. Hiện tại: %d",
                            MAX_STAFF_PER_STATION, staffCountAtStation)
            );
        }

        // Tạo bản ghi phân công
        StaffStationAssignment assignment = new StaffStationAssignment();
        assignment.setStaff(staff);
        assignment.setStation(station);
        // AssignedAt sẽ tự động = thời gian hiện tại trong entity

        return assignmentRepository.save(assignment);
    }

    /**
     * DELETE - Admin thu hồi quyền quản lý trạm của nhân viên
     */
    @Transactional
    public void unassignStaffFromStation(Long staffId, Long stationId) {
        User currentAdmin = authenticationService.getCurrentUser();

        //  Chỉ Admin mới có quyền thu hồi quyền
        if (currentAdmin.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Từ chối truy cập. Chỉ quản trị viên mới có quyền thu hồi nhân viên khỏi trạm.");
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên."));

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm."));

        StaffStationAssignment assignment = assignmentRepository.findByStaffAndStation(staff, station)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công. Nhân viên này chưa được gán vào trạm."));

        assignmentRepository.delete(assignment);
    }

    /**
     * READ - Lấy tất cả trạm mà một nhân viên quản lý
     */
    @Transactional(readOnly = true)
    public List<Station> getStationsByStaff(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên."));

        return assignmentRepository.findStationsByStaff(staff);
    }

    /**
     * READ - Lấy tất cả nhân viên của một trạm (chỉ Admin)
     */
    @Transactional(readOnly = true)
    public List<User> getStaffByStation(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Từ chối truy cập.");
        }

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm."));

        return assignmentRepository.findStaffByStation(station);
    }

    /**
     * READ - Lấy tất cả phân công (chỉ Admin)
     */
    @Transactional(readOnly = true)
    public List<StaffStationAssignment> getAllAssignments() {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Từ chối truy cập.");
        }

        return assignmentRepository.findAll();
    }

    /**
     * READ - Lấy danh sách các trạm mà nhân viên hiện tại đang quản lý
     */
    @Transactional(readOnly = true)
    public List<Station> getMyAssignedStations() {
        User currentStaff = authenticationService.getCurrentUser();

        if (currentStaff.getRole() != User.Role.STAFF) {
            throw new AuthenticationException("Chỉ nhân viên mới được xem danh sách trạm của mình.");
        }

        return assignmentRepository.findStationsByStaff(currentStaff);
    }

    /**
     * READ - Lấy tất cả phân công của một nhân viên
     */
    @Transactional(readOnly = true)
    public List<StaffStationAssignment> getAssignmentsByStaff(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên."));

        return assignmentRepository.findByStaff(staff);
    }

    /**
     * HELPER - Kiểm tra nhân viên có quyền quản lý trạm không
     */
    public boolean canStaffManageStation(User staff, Station station) {
        // Admin có quyền quản lý tất cả
        if (staff.getRole() == User.Role.ADMIN) {
            return true;
        }

        // Nhân viên chỉ được quản lý trạm đã được gán
        if (staff.getRole() == User.Role.STAFF) {
            return assignmentRepository.existsByStaffAndStation(staff, station);
        }

        return false;
    }

    /**
     * VALIDATION - Kiểm tra người dùng hiện tại có quyền truy cập trạm không
     * Ném ra ngoại lệ nếu không có quyền
     */
    public void validateStationAccess(Long stationId) {
        User currentUser = authenticationService.getCurrentUser();

        // Admin có quyền truy cập tất cả
        if (currentUser.getRole() == User.Role.ADMIN) {
            return;
        }

        // Nhân viên chỉ được truy cập trạm được gán
        if (currentUser.getRole() == User.Role.STAFF) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm với ID: " + stationId));

            boolean hasAccess = assignmentRepository.existsByStaffAndStation(currentUser, station);

            if (!hasAccess) {
                throw new AuthenticationException(
                        "Từ chối truy cập. Bạn không được gán để quản lý trạm này (ID: " + stationId + ")."
                );
            }
            return;
        }

        throw new AuthenticationException("Từ chối truy cập.");
    }
}
