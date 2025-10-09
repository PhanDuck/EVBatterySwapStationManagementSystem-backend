package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p WHERE " +
            "(p.transaction IS NOT NULL AND p.transaction.driver.id = :driverId) OR " +
            "(p.subscription IS NOT NULL AND p.subscription.driver.id = :driverId)")
    List<Payment> findByDriverId(@Param("driverId") Long driverId);
}