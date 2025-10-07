package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "StationInventory")
@Getter
@Setter
public class StationInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StationInventoryID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "StationID", nullable = false)
    @JsonIgnore
    private Station station;

    @ManyToOne
    @JoinColumn(name = "BatteryID", nullable = false)
    @JsonIgnore
    private Battery battery;

    @Column(name = "Status", length = 50)
    private String status = "Available";

    @Column(name = "LastUpdate")
    private LocalDateTime lastUpdate = LocalDateTime.now();
}