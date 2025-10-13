package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN p.transaction t " +
            "LEFT JOIN p.subscription s " +
            "WHERE (t IS NOT NULL AND t.driver.id = :driverId) OR " +
            "(s IS NOT NULL AND s.driver.id = :driverId)")
    List<Payment> findByDriverId(@Param("driverId") Long driverId);
}