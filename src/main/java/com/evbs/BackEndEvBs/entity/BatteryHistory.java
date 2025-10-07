package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "BatteryHistory")
@Getter
@Setter
public class BatteryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "BatteryID", nullable = false)
    @JsonIgnore
    private Battery battery;

    @Column(name = "EventType", nullable = false, length = 50)
    private String eventType;

    @Column(name = "EventTime", nullable = false)
    private LocalDateTime eventTime;

    @ManyToOne
    @JoinColumn(name = "RelatedStationID")
    @JsonIgnore
    private Station relatedStation;

    @ManyToOne
    @JoinColumn(name = "RelatedVehicleID")
    @JsonIgnore
    private Vehicle relatedVehicle;

    @ManyToOne
    @JoinColumn(name = "StaffID")
    @JsonIgnore
    private User staff;
}