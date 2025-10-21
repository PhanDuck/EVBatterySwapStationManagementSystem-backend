package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.DriverSubscription;
import com.evbs.BackEndEvBs.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverSubscriptionRepository extends JpaRepository<DriverSubscription, Long> {

    @EntityGraph(attributePaths = {"driver", "servicePackage"})
    List<DriverSubscription> findAll();

    @EntityGraph(attributePaths = {"driver", "servicePackage"})
    List<DriverSubscription> findByDriver_Id(Long driverId);

    // Tìm subscription ACTIVE của driver (có remainingSwaps > 0 và chưa hết hạn)
    // Lấy subscription gần hết hạn nhất (sử dụng trước subscription còn lâu)
    // Dùng findFirst để đảm bảo chỉ lấy 1 kết quả dù có nhiều subscriptions
    @Query("SELECT ds FROM DriverSubscription ds WHERE ds.driver = :driver " +
           "AND ds.status = 'ACTIVE' " +
           "AND ds.remainingSwaps > 0 " +
           "AND ds.endDate >= :currentDate " +
           "ORDER BY ds.endDate ASC")
    List<DriverSubscription> findActiveSubscriptionsByDriver(
            @Param("driver") User driver,
            @Param("currentDate") LocalDate currentDate
    );
    
    // Helper method to get first active subscription
    default Optional<DriverSubscription> findActiveSubscriptionByDriver(User driver, LocalDate currentDate) {
        List<DriverSubscription> subscriptions = findActiveSubscriptionsByDriver(driver, currentDate);
        return subscriptions.isEmpty() ? Optional.empty() : Optional.of(subscriptions.get(0));
    }
}