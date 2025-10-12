package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    // Tìm station theo name
    List<Station> findByNameContainingIgnoreCase(String name);

    // Tìm station theo location
    List<Station> findByLocationContainingIgnoreCase(String location);

    // Tìm station theo status
    List<Station> findByStatus(Station.Status status);

    // Kiểm tra trùng tên station
    boolean existsByName(String name);
}
