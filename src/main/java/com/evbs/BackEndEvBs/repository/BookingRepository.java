package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.Station;
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

    // Tim bookings cua nhieu tram (cho Staff xem bookings cua cac tram minh quan ly)
    List<Booking> findByStationIn(List<Station> stations);

    // Tìm bookings theo status
    List<Booking> findByStatus(Booking.Status status);

    // Tìm booking của driver cụ thể
    Optional<Booking> findByIdAndDriver(Long id, User driver);

    //  Tìm booking bằng confirmationCode (cho staff xác nhận)
    Optional<Booking> findByConfirmationCode(String confirmationCode);

    //dòng này để kiểm tra các booking "chưa kết thúc" của driver
    @Query("SELECT b FROM Booking b WHERE b.driver = :driver AND b.status NOT IN :statuses")
    List<Booking> findByDriverAndStatusNotIn(User driver, List<Booking.Status> statuses);
    
    //  Tìm booking CONFIRMED gần nhất của driver tại station (cho swap transaction)
    @Query("SELECT b FROM Booking b WHERE b.driver = :driver " +
           "AND b.station = :station " +
           "AND b.status = 'CONFIRMED' " +
           "ORDER BY b.bookingTime DESC")
    List<Booking> findConfirmedBookingsByDriverAndStation(
            @Param("driver") User driver,
            @Param("station") Station station
    );
    
    // Helper method to get first confirmed booking
    default Optional<Booking> findLatestConfirmedBooking(User driver, Station station) {
        List<Booking> bookings = findConfirmedBookingsByDriverAndStation(driver, station);
        return bookings.isEmpty() ? Optional.empty() : Optional.of(bookings.get(0));
    }
}

