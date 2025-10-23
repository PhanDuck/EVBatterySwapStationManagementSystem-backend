package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.SupportTicket;
import com.evbs.BackEndEvBs.entity.TicketResponse;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.TicketResponseRequest;
import com.evbs.BackEndEvBs.repository.StaffStationAssignmentRepository;
import com.evbs.BackEndEvBs.repository.SupportTicketRepository;
import com.evbs.BackEndEvBs.repository.TicketResponseRepository;
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
public class TicketResponseService {

    @Autowired
    private final TicketResponseRepository ticketResponseRepository;

    @Autowired
    private final SupportTicketRepository supportTicketRepository;

    @Autowired
    private final StaffStationAssignmentRepository staffStationAssignmentRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    @Autowired
    private final EmailService emailService;
    /**
     * CREATE - Tạo response cho ticket (Staff/Admin only)
     * 
     * VALIDATION:
     * - Admin: Có thể trả lời TẤT CẢ tickets
     * - Staff: Chỉ có thể trả lời tickets của stations mà họ quản lý
     */
    @Transactional
    public TicketResponse createResponse(TicketResponseRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        // Validate ticket trong cùng transaction
        SupportTicket ticket = supportTicketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new NotFoundException("Ticket not found with id: " + request.getTicketId()));

        // Kiểm tra quyền trả lời ticket
        validateTicketAccess(currentUser, ticket);

        // Tạo response mới thay vì dùng modelMapper
        TicketResponse response = new TicketResponse();
        response.setTicket(ticket);
        response.setStaff(currentUser);
        response.setMessage(request.getMessage());
        response.setResponseTime(LocalDateTime.now());

        // Cập nhật status của ticket trong cùng transaction
        ticket.setStatus(SupportTicket.Status.IN_PROGRESS);

        // Lưu ticket trước
        supportTicketRepository.save(ticket);

        // Sau đó lưu response
        TicketResponse savedResponse = ticketResponseRepository.save(response);

        // ===== EMAIL INTEGRATION =====
        try {
            // Gửi email thông báo phản hồi đến Driver
            emailService.sendTicketResponseToDriver(savedResponse);
            log.info("Sent response notification email for ticket: {}", ticket.getId());
        } catch (Exception e) {
            log.error("Failed to send response notification email for ticket: {}",
                    ticket.getId(), e);
            // Không throw exception để không ảnh hưởng đến việc tạo response
        }

        return savedResponse;
    }

    /**
     * READ - Lấy tất cả responses (Admin/Staff only)
     * - Admin: Lấy TẤT CẢ responses
     * - Staff: Chỉ lấy responses của tickets thuộc stations họ quản lý
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllResponses() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        // Admin có thể xem tất cả responses
        if (currentUser.getRole() == User.Role.ADMIN) {
            return ticketResponseRepository.findAll();
        }

        // Staff chỉ xem responses của tickets thuộc stations họ quản lý
        var myStations = staffStationAssignmentRepository.findStationsByStaff(currentUser);
        if (myStations.isEmpty()) {
            throw new AuthenticationException("Staff not assigned to any station");
        }

        return ticketResponseRepository.findByTicketStationIn(myStations);
    }

    /**
     * READ - Lấy responses theo ticket (Admin/Staff only)
     * - Admin: Lấy responses của bất kỳ ticket nào
     * - Staff: Chỉ lấy responses nếu ticket thuộc stations họ quản lý
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getResponsesByTicket(Long ticketId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        // Validate ticket exists
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found with id: " + ticketId));

        // Staff phải check quyền truy cập ticket
        if (currentUser.getRole() == User.Role.STAFF) {
            if (ticket.getStation() == null) {
                throw new AuthenticationException(
                    "Cannot view responses for ticket without station assignment."
                );
            }

            boolean hasAccess = staffStationAssignmentRepository
                .existsByStaffAndStation(currentUser, ticket.getStation());

            if (!hasAccess) {
                throw new AuthenticationException(
                    "Access denied. You can only view responses for tickets from stations you manage."
                );
            }
        }

        return ticketResponseRepository.findByTicketId(ticketId);
    }

    /**
     * READ - Lấy responses của staff hiện tại (Staff/Admin only)
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyResponses() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return ticketResponseRepository.findByStaff_Id(currentUser.getId());
    }

    /**
     * READ - Lấy responses cho ticket của driver (Driver only)
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getResponsesForMyTicket(Long ticketId) {
        User currentUser = authenticationService.getCurrentUser();
        
        // Validate ticket thuộc về driver
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));

        if (!ticket.getDriver().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Ticket does not belong to you");
        }

        return ticketResponseRepository.findByTicketId(ticketId);
    }

    /**
     * UPDATE - Cập nhật response (Staff/Admin only)
     */
    @Transactional
    public TicketResponse updateResponse(Long id, TicketResponseRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        TicketResponse response = ticketResponseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Response not found"));

        // Chỉ cho phép staff tạo response được sửa
        if (!response.getStaff().getId().equals(currentUser.getId())) {
            throw new AuthenticationException("Can only update your own responses");
        }

        if (request.getMessage() != null) {
            response.setMessage(request.getMessage());
        }

        return ticketResponseRepository.save(response);
    }

    /**
     * DELETE - Xóa response (Admin only)
     */
    @Transactional
    public void deleteResponse(Long id) {
        User currentUser = authenticationService.getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AuthenticationException("Access denied");
        }

        TicketResponse response = ticketResponseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Response not found"));
        ticketResponseRepository.delete(response);
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }

    /**
     * Validate quyền trả lời ticket
     * - Admin: Có thể trả lời TẤT CẢ tickets
     * - Staff: Chỉ có thể trả lời tickets của stations họ quản lý
     */
    private void validateTicketAccess(User user, SupportTicket ticket) {
        // Admin có thể trả lời tất cả tickets
        if (user.getRole() == User.Role.ADMIN) {
            return;
        }

        // Staff phải check xem ticket có thuộc station họ quản lý không
        if (user.getRole() == User.Role.STAFF) {
            // Nếu ticket không có station, staff không thể trả lời
            if (ticket.getStation() == null) {
                throw new AuthenticationException(
                    "Cannot respond to ticket without station assignment. " +
                    "Please contact admin to assign this ticket to a station."
                );
            }

            // Kiểm tra staff có được assign cho station của ticket không
            boolean hasAccess = staffStationAssignmentRepository
                .existsByStaffAndStation(user, ticket.getStation());

            if (!hasAccess) {
                throw new AuthenticationException(
                    "Access denied. You can only respond to tickets from stations you manage. " +
                    "This ticket belongs to station: " + ticket.getStation().getName()
                );
            }
        }
    }
}