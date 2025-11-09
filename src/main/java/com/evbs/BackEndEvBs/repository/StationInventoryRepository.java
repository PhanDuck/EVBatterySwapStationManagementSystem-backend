package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.StationInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StationInventoryRepository extends JpaRepository<StationInventory, Long> {
    
    // Tìm StationInventory theo Battery
    Optional<StationInventory> findByBattery(Battery battery);
}