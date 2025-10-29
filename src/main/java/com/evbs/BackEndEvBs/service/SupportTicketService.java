package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.SupportTicket;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.SupportTicketRequest;
import com.evbs.BackEndEvBs.repository.StaffStationAssignmentRepository;
import com.evbs.BackEndEvBs.repository.SupportTicketRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import com.evbs.BackEndEvBs.repository.StaffStationAssignmentRepository;
import com.evbs.BackEndEvBs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {

    @Autowired
    private final SupportTicketRepository supportTicketRepository;

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final StaffStationAssignmentRepository staffStationAssignmentRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final StaffStationAssignmentRepository assignmentRepository;

    @Autowired
    private final EmailService emailService;

    /**
     * CREATE - Tạo support ticket mới (Driver)
     */
    @Transactional
    public SupportTicket createSupportTicket(SupportTicketRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        // Kiểm tra giới hạn 3 tickets OPEN cho driver
        if (currentUser.getRole() == User.Role.DRIVER) {
            long openTickets = supportTicketRepository.countByDriverAndStatus(currentUser, SupportTicket.Status.OPEN);
            if (openTickets >= 3) {
                throw new AuthenticationException("Bạn đã đạt đến giới hạn tối đa 3 ticket hỗ trợ đang mở");
            }
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        ticket.setDriver(currentUser);
        ticket.setStatus(SupportTicket.Status.OPEN);

        // Set station nếu có
        if (request.getStationId() != null) {
            Station station = stationRepository.findById(request.getStationId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));
            ticket.setStation(station);
        }

        ticket.setCreatedAt(LocalDateTime.now());

        // Lưu ticket trước
        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        // ===== EMAIL INTEGRATION =====
        try {
            //Gửi email thông báo đến Staff hoặc Admin dựa trên stationID
            if (request.getStationId() != null) {
                // Có stationID -> Gửi đến Staff của trạm đó
                Station station = stationRepository.findById(request.getStationId()).orElse(null);
                if (station != null) {
                    List<User> stationStaff = assignmentRepository.findStaffByStation(station);

                    if (!stationStaff.isEmpty()) {
                        emailService.sendTicketCreatedToStaff(stationStaff, savedTicket);
                        log.info("Đã gửi thông báo ticket đến {} nhân viên cho trạm: {}",
                                stationStaff.size(), request.getStationId());
                    } else {
                        // Không có staff nào -> Gửi đến Admin
                        List<User> adminList = userRepository.findAll().stream()
                                .filter(user -> user.getRole() == User.Role.ADMIN)
                                .toList();
                        emailService.sendTicketCreatedToAdmin(adminList, savedTicket);
                        log.info("Không tìm thấy nhân viên cho trạm, đã gửi đến {} quản trị viên thay thế", adminList.size());
                    }
                }
            } else {
                // Không có stationID -> Gửi đến Admin
                List<User> adminList = userRepository.findAll().stream()
                        .filter(user -> user.getRole() == User.Role.ADMIN)
                        .toList();
                emailService.sendTicketCreatedToAdmin(adminList, savedTicket);
                log.info("Đã gửi ticket hỗ trợ chung đến {} quản trị viên", adminList.size());
            }

        } catch (Exception e) {
            log.error("Không thể gửi thông báo email cho ticket: {}", savedTicket.getId(), e);
            // Không throw exception để không ảnh hưởng đến việc tạo ticket
        }

        return savedTicket;
    }

    /**
     * READ - Lấy tickets của driver hiện tại
     */
    @Transactional(readOnly = true)
    public List<SupportTicket> getMyTickets() {
        User currentUser = authenticationService.getCurrentUser();
        return supportTicketRepository.findByDriver(currentUser);
    }

    /**
     * READ - Lấy ticket cụ thể của driver
     */
    @Transactional(readOnly = true)
    public SupportTicket getMyTicket(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        return supportTicketRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ticket"));
    }

    /**
     * UPDATE - Cập nhật ticket của driver
     */
    @Transactional
    public SupportTicket updateMyTicket(Long id, SupportTicketRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        SupportTicket ticket = supportTicketRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ticket"));

        // Chỉ cho phép update khi ticket chưa được trả lời
        if (!SupportTicket.Status.OPEN.equals(ticket.getStatus())) {
            throw new AuthenticationException("Không thể cập nhật ticket đang được xử lý");
        }

        // Update các field
        if (request.getSubject() != null) {
            ticket.setSubject(request.getSubject());
        }

        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }

        // Update station nếu có
        if (request.getStationId() != null) {
            Station station = stationRepository.findById(request.getStationId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy trạm"));
            ticket.setStation(station);
        }

        return supportTicketRepository.save(ticket);
    }

    /**
     * DELETE - Xóa ticket của driver
     */
    @Transactional
    public void deleteMyTicket(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        SupportTicket ticket = supportTicketRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ticket"));

        // Kiểm tra xem ticket có responses hay không
        if (!ticket.getResponses().isEmpty()) {
            throw new AuthenticationException("Không thể xóa ticket đã có phản hồi từ nhân viên");
        }

        // Chỉ cho phép xóa ticket có status OPEN
        if (!SupportTicket.Status.OPEN.equals(ticket.getStatus())) {
            throw new AuthenticationException("Chỉ có thể xóa các ticket có trạng thái OPEN");
        }

        supportTicketRepository.delete(ticket);
    }

    /**
     * READ - Lấy tất cả tickets (Admin/Staff only)
     * - Admin: Lấy TẤT CẢ tickets
     * - Staff: Chỉ lấy tickets của các stations họ quản lý
     */
    @Transactional(readOnly = true)
    public List<SupportTicket> getAllTickets() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối");
        }

        // Admin có thể xem tất cả tickets
        if (currentUser.getRole() == User.Role.ADMIN) {
            return supportTicketRepository.findAll();
        }

        // Staff chỉ xem tickets của các station họ quản lý
        List<Station> myStations = staffStationAssignmentRepository.findStationsByStaff(currentUser);
        if (myStations.isEmpty()) {
            throw new AuthenticationException("Nhân viên chưa được phân công vào trạm nào");
        }

        return supportTicketRepository.findByStationIn(myStations);
    }

    /**
     * UPDATE - Cập nhật ticket status (Admin/Staff only)
     * - Admin: Cập nhật TẤT CẢ tickets
     * - Staff: Chỉ cập nhật tickets của stations họ quản lý
     */
    @Transactional
    public SupportTicket updateTicketStatus(Long id, SupportTicket.Status status) {
        User currentUser = authenticationService.getCurrentUser();

        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Truy cập bị từ chối");
        }

        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ticket với id: " + id));

        // Staff phải kiểm tra quyền truy cập ticket
        if (currentUser.getRole() == User.Role.STAFF) {
            // Nếu ticket không có station, staff không thể cập nhật
            if (ticket.getStation() == null) {
                throw new AuthenticationException(
                        "Không thể cập nhật ticket không có trạm được chỉ định. " +
                                "Vui lòng liên hệ quản trị viên."
                );
            }

            // Kiểm tra staff có được assign cho station của ticket không
            boolean hasAccess = staffStationAssignmentRepository
                    .existsByStaffAndStation(currentUser, ticket.getStation());

            if (!hasAccess) {
                throw new AuthenticationException(
                        "Truy cập bị từ chối. Bạn chỉ có thể cập nhật tickets từ các trạm bạn quản lý. " +
                                "Ticket này thuộc về trạm: " + ticket.getStation().getName()
                );
            }
        }

        ticket.setStatus(status);
        return supportTicketRepository.save(ticket);
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}