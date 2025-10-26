package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.TicketResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketResponseRepository extends JpaRepository<TicketResponse, Long> {

    // Tìm responses theo ticket
    List<TicketResponse> findByTicketId(Long ticketId);

    // Tìm responses theo staff (sử dụng nested property)
    List<TicketResponse> findByStaff_Id(Long staffId);

    // Tìm responses của tickets thuộc nhiều stations (cho staff quản lý nhiều trạm)
    @Query("SELECT tr FROM TicketResponse tr WHERE tr.ticket.station IN :stations")
    List<TicketResponse> findByTicketStationIn(@Param("stations") List<Station> stations);
}