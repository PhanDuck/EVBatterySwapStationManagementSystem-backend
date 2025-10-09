package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.DriverSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverSubscriptionRepository extends JpaRepository<DriverSubscription, Long> {
    List<DriverSubscription> findByDriverId(Long driverId);
}