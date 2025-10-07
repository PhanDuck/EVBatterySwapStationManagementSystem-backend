package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Tìm booking theo driver
    List<Booking> findByDriverOrderByBookingTimeDesc(User driver);

    // Tìm booking theo driver ID
    List<Booking> findByDriverIdOrderByBookingTimeDesc(Long driverId);

    // Tìm booking theo vehicle
    List<Booking> findByVehicleOrderByBookingTimeDesc(Vehicle vehicle);

    // Tìm booking theo station
    List<Booking> findByStationOrderByBookingTimeDesc(Station station);

    // Tìm booking theo status
    List<Booking> findByStatusOrderByBookingTimeDesc(String status);

    // Tìm booking theo driver và status
    List<Booking> findByDriverAndStatusOrderByBookingTimeDesc(User driver, String status);

    // Tìm booking theo thời gian
    List<Booking> findByBookingTimeBetweenOrderByBookingTimeDesc(LocalDateTime start, LocalDateTime end);

    // Tìm booking sắp tới của driver
    @Query("SELECT b FROM Booking b WHERE b.driver = :driver AND b.bookingTime >= :now AND b.status IN ('Pending', 'Confirmed') ORDER BY b.bookingTime ASC")
    List<Booking> findUpcomingByDriver(@Param("driver") User driver, @Param("now") LocalDateTime now);

    // Tìm booking hiện tại của station (trong vòng 2 giờ)
    @Query("SELECT b FROM Booking b WHERE b.station = :station AND b.bookingTime BETWEEN :start AND :end AND b.status IN ('Pending', 'Confirmed') ORDER BY b.bookingTime ASC")
    List<Booking> findCurrentByStation(@Param("station") Station station,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    // Đếm số booking confirmed tại station trong khoảng thời gian
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.station = :station AND b.status = 'Confirmed' AND b.bookingTime BETWEEN :start AND :end")
    long countConfirmedByStationAndTimeRange(@Param("station") Station station,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    // Tìm booking theo ID và driver (cho Driver chỉ xem booking của mình)
    Optional<Booking> findByIdAndDriver(Long id, User driver);

    // Tìm booking đang chờ xử lý tại station
    List<Booking> findByStationAndStatusOrderByBookingTimeAsc(Station station, String status);
}