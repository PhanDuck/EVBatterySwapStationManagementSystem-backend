package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Tìm bookings theo driver
    List<Booking> findByDriver(User driver);

    // Tìm bookings theo station
    @Query("SELECT b FROM Booking b WHERE b.station.id = :stationId")
    List<Booking> findByStationId(@Param("stationId") Long stationId);

    // Tìm bookings theo status
    List<Booking> findByStatus(com.evbs.BackEndEvBs.entity.Booking.Status status);

    // Tìm booking của driver cụ thể
    Optional<Booking> findByIdAndDriver(Long id, User driver);

}