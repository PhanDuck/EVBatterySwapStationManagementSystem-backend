package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    // Tìm station theo name (case insensitive)
    List<Station> findByNameContainingIgnoreCase(String name);

    // Tìm station theo location
    List<Station> findByLocationContainingIgnoreCase(String location);

    // Tìm station theo status
    List<Station> findByStatus(String status);

    // Tìm active stations
    List<Station> findByStatusOrderByNameAsc(String status);

    // Tìm station theo capacity range
    @Query("SELECT s FROM Station s WHERE s.capacity BETWEEN :minCapacity AND :maxCapacity")
    List<Station> findByCapacityBetween(@Param("minCapacity") Integer minCapacity,
                                        @Param("maxCapacity") Integer maxCapacity);

    // Tìm stations có available batteries
    @Query("SELECT DISTINCT s FROM Station s JOIN s.inventory i WHERE i.status = 'Available'")
    List<Station> findStationsWithAvailableBatteries();

    // Đếm số stations theo status
    long countByStatus(String status);

    // Kiểm tra trùng tên station (cho create/update)
    boolean existsByName(String name);

    // Tìm station by name (exact match)
    Optional<Station> findByName(String name);
}