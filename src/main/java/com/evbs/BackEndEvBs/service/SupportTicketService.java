package com.evbs.BackEndEvBs.service;

import com.evbs.BackEndEvBs.entity.SupportTicket;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.exception.exceptions.AuthenticationException;
import com.evbs.BackEndEvBs.exception.exceptions.NotFoundException;
import com.evbs.BackEndEvBs.model.request.SupportTicketRequest;
import com.evbs.BackEndEvBs.repository.SupportTicketRepository;
import com.evbs.BackEndEvBs.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
    private final AuthenticationService authenticationService;

    @Autowired
    private final ModelMapper modelMapper;

    /**
     * CREATE - Tạo support ticket mới (Driver)
     */
    @Transactional
    public SupportTicket createSupportTicket(SupportTicketRequest request) {
        User currentUser = authenticationService.getCurrentUser();

        SupportTicket ticket = modelMapper.map(request, SupportTicket.class);
        ticket.setDriver(currentUser);

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
        if (!ticket.getStatus().equals("Open")) {
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

        supportTicketRepository.delete(ticket);
    }

    /**
     * READ - Lấy tất cả tickets (Admin/Staff only)
     */
    @Transactional(readOnly = true)
    public List<SupportTicket> getAllTickets() {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }
        return supportTicketRepository.findAll();
    }

    /**
     * UPDATE - Cập nhật ticket status (Admin/Staff only)
     */
    @Transactional
    public SupportTicket updateTicketStatus(Long id, String status) {
        User currentUser = authenticationService.getCurrentUser();
        if (!isAdminOrStaff(currentUser)) {
            throw new AuthenticationException("Access denied");
        }

        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));

        ticket.setStatus(status);
        return supportTicketRepository.save(ticket);
    }

    // ==================== HELPER METHODS ====================

    private boolean isAdminOrStaff(User user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.STAFF;
    }
}