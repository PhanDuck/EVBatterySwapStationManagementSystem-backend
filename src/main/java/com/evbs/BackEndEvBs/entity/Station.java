package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Station")
@Getter
@Setter
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StationID")
    private Long id;

    @NotEmpty(message = "Name cannot be empty!")
    @Column(name = "Name", nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
    private String name;

    @Column(name = "Location", length = 255, columnDefinition = "NVARCHAR(255)")
    private String location;

    @Column(name = "Capacity")
    private Integer capacity;

    @Column(name = "ContactInfo", length = 150, columnDefinition = "NVARCHAR(150)")
    private String contactInfo;
    // Tọa độ GPS
    @Column(name = "Latitude")
    private Double latitude;

    @Column(name = "Longitude")
    private Double longitude;

    @Column(name = "Status", length = 50)
    private String status = "Active";

    // Relationships
    @OneToMany(mappedBy = "currentStation")
    @JsonIgnore
    private List<Battery> batteries = new ArrayList<>();

    @OneToMany(mappedBy = "station")
    @JsonIgnore
    private List<StationInventory> inventory = new ArrayList<>();

    @OneToMany(mappedBy = "station")
    @JsonIgnore
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "station")
    @JsonIgnore
    private List<SwapTransaction> swapTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "station")
    @JsonIgnore
    private List<SupportTicket> supportTickets = new ArrayList<>();

    @OneToMany(mappedBy = "relatedStation")
    @JsonIgnore
    private List<BatteryHistory> batteryHistories = new ArrayList<>();
}
