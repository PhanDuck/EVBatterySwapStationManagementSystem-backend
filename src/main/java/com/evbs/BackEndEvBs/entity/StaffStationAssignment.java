package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity quản lý phân công Staff cho Station
 * - 1 Staff có thể quản lý nhiều Station (3-5 stations)
 * - 1 Station có thể được quản lý bởi nhiều Staff
 * - Chỉ Admin mới có thể assign/unassign
 */
@Entity
@Table(name = "StaffStationAssignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"StaffID", "StationID"}))
@Getter
@Setter
public class StaffStationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AssignmentID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "StaffID", nullable = false)
    @JsonIgnore
    private User staff;

    @Transient
    private Long staffId;

    @ManyToOne
    @JoinColumn(name = "StationID", nullable = false)
    @JsonIgnore
    private Station station;

    @Transient
    private Long stationId;

    @Column(name = "AssignedAt", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    // Getters for @Transient fields
    public Long getStaffId() {
        return staff != null ? staff.getId() : null;
    }

    public Long getStationId() {
        return station != null ? station.getId() : null;
    }
}
