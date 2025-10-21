package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.StationInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StationInventoryRepository extends JpaRepository<StationInventory, Long> {
}