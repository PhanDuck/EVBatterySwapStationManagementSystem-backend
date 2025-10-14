package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.DriverSubscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverSubscriptionRepository extends JpaRepository<DriverSubscription, Long> {

    @EntityGraph(attributePaths = {"driver", "servicePackage"})
    List<DriverSubscription> findAll();

    @EntityGraph(attributePaths = {"driver", "servicePackage"})
    List<DriverSubscription> findByDriver_Id(Long driverId);
}