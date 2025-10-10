package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.SwapTransaction;
import com.evbs.BackEndEvBs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SwapTransactionRepository extends JpaRepository<SwapTransaction, Long> {

    // Tìm transactions theo driver
    List<SwapTransaction> findByDriver(User driver);

    // Tìm transactions theo station
    List<SwapTransaction> findByStationId(Long stationId);

    // Tìm transactions theo status
    List<SwapTransaction> findByStatus(String status);

    // Tìm transaction của driver cụ thể
    Optional<SwapTransaction> findByIdAndDriver(Long id, User driver);
}