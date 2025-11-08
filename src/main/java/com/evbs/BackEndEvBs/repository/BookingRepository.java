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

    // Tìm bookings theo driver với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.driver " +
           "LEFT JOIN FETCH b.vehicle " +
           "LEFT JOIN FETCH b.station " +
           "LEFT JOIN FETCH b.reservedBattery " +
           "LEFT JOIN FETCH b.confirmedBy " +
           "LEFT JOIN FETCH b.swapTransaction st " +
           "LEFT JOIN FETCH st.swapOutBattery " +
           "LEFT JOIN FETCH st.swapInBattery " +
           "WHERE b.driver = :driver")
    List<Booking> findByDriverWithDetails(@Param("driver") User driver);

    // Tìm TẤT CẢ bookings với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.driver " +
           "LEFT JOIN FETCH b.vehicle " +
           "LEFT JOIN FETCH b.station " +
           "LEFT JOIN FETCH b.reservedBattery " +
           "LEFT JOIN FETCH b.confirmedBy " +
           "LEFT JOIN FETCH b.swapTransaction st " +
           "LEFT JOIN FETCH st.swapOutBattery " +
           "LEFT JOIN FETCH st.swapInBattery")
    List<Booking> findAllWithDetails();

    // Tìm bookings theo station
    @Query("SELECT b FROM Booking b WHERE b.station.id = :stationId")
    List<Booking> findByStationId(@Param("stationId") Long stationId);

    // Tìm bookings theo station với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.driver " +
           "LEFT JOIN FETCH b.vehicle " +
           "LEFT JOIN FETCH b.station " +
           "LEFT JOIN FETCH b.reservedBattery " +
           "LEFT JOIN FETCH b.confirmedBy " +
           "LEFT JOIN FETCH b.swapTransaction st " +
           "LEFT JOIN FETCH st.swapOutBattery " +
           "LEFT JOIN FETCH st.swapInBattery " +
           "WHERE b.station.id = :stationId")
    List<Booking> findByStationIdWithDetails(@Param("stationId") Long stationId);

    // Tim bookings cua nhieu tram (cho Staff xem bookings cua cac tram minh quan ly)
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.driver " +
           "LEFT JOIN FETCH b.vehicle " +
           "LEFT JOIN FETCH b.station " +
           "LEFT JOIN FETCH b.reservedBattery " +
           "LEFT JOIN FETCH b.confirmedBy " +
           "LEFT JOIN FETCH b.swapTransaction st " +
           "LEFT JOIN FETCH st.swapOutBattery " +
           "LEFT JOIN FETCH st.swapInBattery " +
           "WHERE b.station IN :stations")
    List<Booking> findByStationInWithDetails(@Param("stations") List<Station> stations);

    List<Booking> findByStationIn(List<Station> stations);

    // Tìm bookings theo status
    List<Booking> findByStatus(Booking.Status status);

    // Tìm bookings theo status với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.driver " +
           "LEFT JOIN FETCH b.vehicle " +
           "LEFT JOIN FETCH b.station " +
           "LEFT JOIN FETCH b.reservedBattery " +
           "LEFT JOIN FETCH b.confirmedBy " +
           "LEFT JOIN FETCH b.swapTransaction st " +
           "LEFT JOIN FETCH st.swapOutBattery " +
           "LEFT JOIN FETCH st.swapInBattery " +
           "WHERE b.status = :status")
    List<Booking> findByStatusWithDetails(@Param("status") Booking.Status status);

    // Tìm booking của driver cụ thể
    Optional<Booking> findByIdAndDriver(Long id, User driver);

    // Tìm booking của driver cụ thể với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.driver " +
           "LEFT JOIN FETCH b.vehicle " +
           "LEFT JOIN FETCH b.station " +
           "LEFT JOIN FETCH b.reservedBattery " +
           "LEFT JOIN FETCH b.confirmedBy " +
           "LEFT JOIN FETCH b.swapTransaction st " +
           "LEFT JOIN FETCH st.swapOutBattery " +
           "LEFT JOIN FETCH st.swapInBattery " +
           "WHERE b.id = :id AND b.driver = :driver")
    Optional<Booking> findByIdAndDriverWithDetails(@Param("id") Long id, @Param("driver") User driver);

    //  Tìm booking bằng confirmationCode (cho staff xác nhận)
    Optional<Booking> findByConfirmationCode(String confirmationCode);

    //dòng này để kiểm tra các booking "chưa kết thúc" của driver
    @Query("SELECT b FROM Booking b WHERE b.driver = :driver AND b.status NOT IN :statuses")
    List<Booking> findByDriverAndStatusNotIn(User driver, List<Booking.Status> statuses);

    // KIỂM TRA: 1 xe chỉ có 1 booking active tại 1 thời điểm
    @Query("SELECT b FROM Booking b WHERE b.vehicle = :vehicle AND b.status NOT IN :statuses")
    List<Booking> findByVehicleAndStatusNotIn(
            @Param("vehicle") com.evbs.BackEndEvBs.entity.Vehicle vehicle,
            @Param("statuses") List<Booking.Status> statuses
    );
    
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

