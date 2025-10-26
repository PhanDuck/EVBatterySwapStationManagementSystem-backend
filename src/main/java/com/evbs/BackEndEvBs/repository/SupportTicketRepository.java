package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.SupportTicket;
import com.evbs.BackEndEvBs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    // Tìm tickets theo driver
    List<SupportTicket> findByDriver(User driver);

    // Tìm ticket của driver cụ thể
    Optional<SupportTicket> findByIdAndDriver(Long id, User driver);

    // Đếm tickets theo driver và status
    long countByDriverAndStatus(User driver, SupportTicket.Status status);

    // Tìm tickets theo station
    List<SupportTicket> findByStation(Station station);

    // Tìm tickets theo nhiều stations (cho staff quản lý nhiều trạm)
    @Query("SELECT t FROM SupportTicket t WHERE t.station IN :stations")
    List<SupportTicket> findByStationIn(@Param("stations") List<Station> stations);
}