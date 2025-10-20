package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.TicketResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketResponseRepository extends JpaRepository<TicketResponse, Long> {

    // Tìm responses theo ticket
    List<TicketResponse> findByTicketId(Long ticketId);

    // Tìm responses theo staff (sử dụng nested property)
    List<TicketResponse> findByStaff_Id(Long staffId);
}