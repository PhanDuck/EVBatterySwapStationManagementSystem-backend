package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.SupportTicket;
import com.evbs.BackEndEvBs.entity.TicketResponse;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.TicketResponseRequest;
import com.evbs.BackEndEvBs.repository.SupportTicketRepository;
import com.evbs.BackEndEvBs.repository.TicketResponseRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketResponseService {

    @Autowired
    private final TicketResponseRepository ticketResponseRepository;

    @Autowired
    private final SupportTicketRepository supportTicketRepository;

    @Autowired
    private final AuthenticationService authenticationService;

    /**
     * CREATE - Tạo response cho ticket (Staff/Admin only)
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
        return ticketResponseRepository.save(response);
    }

    /**
     * READ - Lấy tất cả responses (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllResponses() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return ticketResponseRepository.findAll();
    }

    /**
     * READ - Lấy responses theo ticket (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getResponsesByTicket(Long ticketId) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
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
}