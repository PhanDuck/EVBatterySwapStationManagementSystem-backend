package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Booking;
import com.evbs.BackEndEvBs.entity.SwapTransaction;
import com.evbs.BackEndEvBs.entity.User;
import com.evbs.BackEndEvBs.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SwapTransactionRepository extends JpaRepository<SwapTransaction, Long> {

    // Tìm transactions theo driver
    List<SwapTransaction> findByDriver(User driver);

    // Tìm transaction của driver cụ thể
    Optional<SwapTransaction> findByIdAndDriver(Long id, User driver);

    // tìm swap transaction gần nhất của vehicle (để biết pin nào đang trên xe)
    Optional<SwapTransaction> findTopByVehicleOrderByStartTimeDesc(Vehicle vehicle);

    //  Tìm tất cả swap transactions của vehicle (lịch sử đổi pin)
    List<SwapTransaction> findByVehicleOrderByStartTimeDesc(Vehicle vehicle);
    
    //  Tìm swap transaction theo booking (kiểm tra code đã dùng chưa)
    Optional<SwapTransaction> findByBooking(Booking booking);
    
    //  Tìm tất cả lần pin được lấy ra từ trạm (pin đã dùng cho xe nào)
    List<SwapTransaction> findBySwapOutBatteryOrderByStartTimeDesc(com.evbs.BackEndEvBs.entity.Battery battery);
    
    //  Tìm tất cả lần pin được đem vào trạm (pin được trả lại)
    List<SwapTransaction> findBySwapInBatteryOrderByStartTimeDesc(com.evbs.BackEndEvBs.entity.Battery battery);
}