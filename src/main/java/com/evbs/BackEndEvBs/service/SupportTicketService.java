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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    @Autowired
    private final SupportTicketRepository supportTicketRepository;

    @Autowired
    private final StationRepository stationRepository;

    @Autowired
    private final StaffStationAssignmentRepository staffStationAssignmentRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    /**
     * CREATE - Tạo support ticket mới (Driver)
     */
    @Transactional
    public SupportTicket createSupportTicket(SupportTicketRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        // Kiểm tra giới hạn 5 tickets OPEN cho driver
        if (currentUser.getRole() == User.Role.DRIVER) {
            long openTickets = supportTicketRepository.countByDriverAndStatus(currentUser, SupportTicket.Status.OPEN);
            if (openTickets >= 5) {
                throw new AuthenticationException("You have reached the maximum limit of 5 open support tickets");
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
                    .orElseThrow(() -> new NotFoundException("Station not found"));
            ticket.setStation(station);
        }

        ticket.setCreatedAt(LocalDateTime.now());

        return supportTicketRepository.save(ticket);
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
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
    }

    /**
     * UPDATE - Cập nhật ticket của driver
     */
    @Transactional
    public SupportTicket updateMyTicket(Long id, SupportTicketRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        SupportTicket ticket = supportTicketRepository.findByIdAndDriver(id, currentUser)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));

        // Chỉ cho phép update khi ticket chưa được trả lời
        if (!SupportTicket.Status.OPEN.equals(ticket.getStatus())) {
            throw new AuthenticationException("Cannot update ticket that is already being processed");
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
                    .orElseThrow(() -> new NotFoundException("Station not found"));
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
                .orElseThrow(() -> new NotFoundException("Ticket not found"));

        // Kiểm tra xem ticket có responses hay không
        if (!ticket.getResponses().isEmpty()) {
            throw new AuthenticationException("Cannot delete ticket that has responses from staff");
        }

        // Chỉ cho phép xóa ticket có status OPEN
        if (!SupportTicket.Status.OPEN.equals(ticket.getStatus())) {
            throw new AuthenticationException("Can only delete tickets with OPEN status");
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
            throw new AuthenticationException("Access denied");
        }
        
        // Admin có thể xem tất cả tickets
        if (currentUser.getRole() == User.Role.ADMIN) {
            return supportTicketRepository.findAll();
        }
        
        // Staff chỉ xem tickets của các station họ quản lý
        List<Station> myStations = staffStationAssignmentRepository.findStationsByStaff(currentUser);
        if (myStations.isEmpty()) {
            throw new AuthenticationException("Staff not assigned to any station");
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
            throw new AuthenticationException("Access denied");
        }

        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket not found with id: " + id));

        // Staff phải kiểm tra quyền truy cập ticket
        if (currentUser.getRole() == User.Role.STAFF) {
            // Nếu ticket không có station, staff không thể cập nhật
            if (ticket.getStation() == null) {
                throw new AuthenticationException(
                    "Cannot update ticket without station assignment. " +
                    "Please contact admin."
                );
            }

            // Kiểm tra staff có được assign cho station của ticket không
            boolean hasAccess = staffStationAssignmentRepository
                .existsByStaffAndStation(currentUser, ticket.getStation());

            if (!hasAccess) {
                throw new AuthenticationException(
                    "Access denied. You can only update tickets from stations you manage. " +
                    "This ticket belongs to station: " + ticket.getStation().getName()
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