package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN p.transaction t " +
            "LEFT JOIN p.subscription s " +
            "WHERE (t IS NOT NULL AND t.driver.id = :driverId) OR " +
            "(s IS NOT NULL AND s.driver.id = :driverId)")
    List<Payment> findByDriverId(@Param("driverId") Long driverId);

    // Dashboard queries - Tổng doanh thu
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal sumTotalRevenue();

    // Doanh thu theo khoảng thời gian
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
