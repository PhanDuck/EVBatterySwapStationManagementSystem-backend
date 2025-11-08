package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.SwapTransaction;
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
public interface SwapTransactionRepository extends JpaRepository<SwapTransaction, Long> {

    // Tìm transactions theo driver
    List<SwapTransaction> findByDriver(User driver);

    // Tìm transactions theo driver với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT st FROM SwapTransaction st " +
           "LEFT JOIN FETCH st.driver " +
           "LEFT JOIN FETCH st.vehicle " +
           "LEFT JOIN FETCH st.station " +
           "LEFT JOIN FETCH st.staff " +
           "LEFT JOIN FETCH st.swapOutBattery sob " +
           "LEFT JOIN FETCH sob.batteryType " +
           "LEFT JOIN FETCH st.swapInBattery sib " +
           "LEFT JOIN FETCH sib.batteryType " +
           "LEFT JOIN FETCH st.booking " +
           "WHERE st.driver = :driver")
    List<SwapTransaction> findByDriverWithDetails(@Param("driver") User driver);

    // Tìm transaction của driver cụ thể
    Optional<SwapTransaction> findByIdAndDriver(Long id, User driver);

    // Tìm transaction của driver cụ thể với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT st FROM SwapTransaction st " +
           "LEFT JOIN FETCH st.driver " +
           "LEFT JOIN FETCH st.vehicle " +
           "LEFT JOIN FETCH st.station " +
           "LEFT JOIN FETCH st.staff " +
           "LEFT JOIN FETCH st.swapOutBattery sob " +
           "LEFT JOIN FETCH sob.batteryType " +
           "LEFT JOIN FETCH st.swapInBattery sib " +
           "LEFT JOIN FETCH sib.batteryType " +
           "LEFT JOIN FETCH st.booking " +
           "WHERE st.id = :id AND st.driver = :driver")
    Optional<SwapTransaction> findByIdAndDriverWithDetails(@Param("id") Long id, @Param("driver") User driver);

    // tìm swap transaction gần nhất của vehicle (để biết pin nào đang trên xe)
    Optional<SwapTransaction> findTopByVehicleOrderByStartTimeDesc(Vehicle vehicle);

    //  Tìm tất cả swap transactions của vehicle (lịch sử đổi pin)
    List<SwapTransaction> findByVehicleOrderByStartTimeDesc(Vehicle vehicle);

    //  Tìm tất cả swap transactions của vehicle với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT st FROM SwapTransaction st " +
           "LEFT JOIN FETCH st.driver " +
           "LEFT JOIN FETCH st.vehicle " +
           "LEFT JOIN FETCH st.station " +
           "LEFT JOIN FETCH st.staff " +
           "LEFT JOIN FETCH st.swapOutBattery sob " +
           "LEFT JOIN FETCH sob.batteryType " +
           "LEFT JOIN FETCH st.swapInBattery sib " +
           "LEFT JOIN FETCH sib.batteryType " +
           "LEFT JOIN FETCH st.booking " +
           "WHERE st.vehicle = :vehicle " +
           "ORDER BY st.startTime DESC")
    List<SwapTransaction> findByVehicleWithDetailsOrderByStartTimeDesc(@Param("vehicle") Vehicle vehicle);

    //  Tìm swap transaction theo booking (kiểm tra code đã dùng chưa)
    Optional<SwapTransaction> findByBooking(Booking booking);

    //  Tìm tất cả lần pin được lấy ra từ trạm (pin đã dùng cho xe nào)
    List<SwapTransaction> findBySwapOutBatteryOrderByStartTimeDesc(com.evbs.BackEndEvBs.entity.Battery battery);

    //  Tìm tất cả lần pin được lấy ra từ trạm với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT st FROM SwapTransaction st " +
           "LEFT JOIN FETCH st.driver " +
           "LEFT JOIN FETCH st.vehicle " +
           "LEFT JOIN FETCH st.station " +
           "LEFT JOIN FETCH st.staff " +
           "LEFT JOIN FETCH st.swapOutBattery sob " +
           "LEFT JOIN FETCH sob.batteryType " +
           "LEFT JOIN FETCH st.swapInBattery sib " +
           "LEFT JOIN FETCH sib.batteryType " +
           "LEFT JOIN FETCH st.booking " +
           "WHERE st.swapOutBattery = :battery " +
           "ORDER BY st.startTime DESC")
    List<SwapTransaction> findBySwapOutBatteryWithDetailsOrderByStartTimeDesc(@Param("battery") com.evbs.BackEndEvBs.entity.Battery battery);

    //  Tìm tất cả lần pin được đem vào trạm (pin được trả lại)
    List<SwapTransaction> findBySwapInBatteryOrderByStartTimeDesc(com.evbs.BackEndEvBs.entity.Battery battery);

    //  Tìm tất cả lần pin được đem vào trạm với JOIN FETCH để tránh N+1 query
    @Query("SELECT DISTINCT st FROM SwapTransaction st " +
           "LEFT JOIN FETCH st.driver " +
           "LEFT JOIN FETCH st.vehicle " +
           "LEFT JOIN FETCH st.station " +
           "LEFT JOIN FETCH st.staff " +
           "LEFT JOIN FETCH st.swapOutBattery sob " +
           "LEFT JOIN FETCH sob.batteryType " +
           "LEFT JOIN FETCH st.swapInBattery sib " +
           "LEFT JOIN FETCH sib.batteryType " +
           "LEFT JOIN FETCH st.booking " +
           "WHERE st.swapInBattery = :battery " +
           "ORDER BY st.startTime DESC")
    List<SwapTransaction> findBySwapInBatteryWithDetailsOrderByStartTimeDesc(@Param("battery") com.evbs.BackEndEvBs.entity.Battery battery);

    // Tìm TẤT CẢ transactions với JOIN FETCH để tránh N+1 query (cho getAllTransactions)
    @Query("SELECT DISTINCT st FROM SwapTransaction st " +
           "LEFT JOIN FETCH st.driver " +
           "LEFT JOIN FETCH st.vehicle " +
           "LEFT JOIN FETCH st.station " +
           "LEFT JOIN FETCH st.staff " +
           "LEFT JOIN FETCH st.swapOutBattery sob " +
           "LEFT JOIN FETCH sob.batteryType " +
           "LEFT JOIN FETCH st.swapInBattery sib " +
           "LEFT JOIN FETCH sib.batteryType " +
           "LEFT JOIN FETCH st.booking")
    List<SwapTransaction> findAllWithDetails();

    // Dashboard queries - Đếm theo status
    Long countByStatus(SwapTransaction.Status status);

       // Đếm số giao dịch theo danh sách vehicleId (trả về list of [vehicleId, count])
       @Query("SELECT st.vehicle.id, COUNT(st) FROM SwapTransaction st WHERE st.vehicle.id IN :ids GROUP BY st.vehicle.id")
       List<Object[]> countByVehicleIds(@Param("ids") List<Long> ids);

    // Đếm theo khoảng thời gian
    Long countByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Tính thời gian đổi pin trung bình (phút) - Native query cho SQL Server
    @Query(value = "SELECT AVG(CAST(DATEDIFF(MINUTE, StartTime, EndTime) AS FLOAT)) FROM SwapTransaction WHERE EndTime IS NOT NULL", nativeQuery = true)
    Double calculateAverageSwapTime();

    // Lấy N giao dịch gần nhất
    List<SwapTransaction> findTop10ByOrderByStartTimeDesc();

    // Top stations theo số giao dịch - JPQL
    @Query("SELECT t.station, COUNT(t), COALESCE(SUM(t.cost), 0) FROM SwapTransaction t WHERE t.startTime >= :startDate GROUP BY t.station ORDER BY COUNT(t) DESC")
    List<Object[]> findTopStationsByTransactionCount(@Param("startDate") LocalDateTime startDate);

    // Đếm giao dịch theo giờ - Native query cho SQL Server
    @Query(value = "SELECT DATEPART(HOUR, StartTime) as hour, COUNT(*) as cnt " +
            "FROM SwapTransaction " +
            "WHERE StartTime BETWEEN :startDate AND :endDate " +
            "GROUP BY DATEPART(HOUR, StartTime) " +
            "ORDER BY DATEPART(HOUR, StartTime)",
            nativeQuery = true)
    List<Object[]> countTransactionsByHour(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}