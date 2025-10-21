package com.evbs.BackEndEvBs.repository;

import com.evbs.BackEndEvBs.entity.StaffStationAssignment;
import com.evbs.BackEndEvBs.entity.Station;
import com.evbs.BackEndEvBs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffStationAssignmentRepository extends JpaRepository<StaffStationAssignment, Long> {

    /**
     * Tìm tất cả stations được assign cho 1 staff
     */
    @Query("SELECT ssa.station FROM StaffStationAssignment ssa WHERE ssa.staff = :staff")
    List<Station> findStationsByStaff(@Param("staff") User staff);

    /**
     * Tìm tất cả staff được assign cho 1 station
     */
    @Query("SELECT ssa.staff FROM StaffStationAssignment ssa WHERE ssa.station = :station")
    List<User> findStaffByStation(@Param("station") Station station);

    /**
     * Kiểm tra staff đã được assign cho station chưa
     */
    boolean existsByStaffAndStation(User staff, Station station);

    /**
     * Tìm assignment cụ thể
     */
    Optional<StaffStationAssignment> findByStaffAndStation(User staff, Station station);

    /**
     * Đếm số stations của 1 staff
     */
    long countByStaff(User staff);

    /**
     * Đếm số staff của 1 station
     */
    long countByStation(Station station);

    /**
     * Lấy tất cả assignments của 1 staff
     */
    List<StaffStationAssignment> findByStaff(User staff);
}
