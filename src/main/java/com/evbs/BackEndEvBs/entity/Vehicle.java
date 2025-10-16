package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Vehicle")
@Getter
@Setter
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VehicleID")
    private Long id;

    @Column(name = "VIN", nullable = false, unique = true, length = 100)
    private String vin;

    @Column(name = "PlateNumber", nullable = false, unique = true, length = 50)
    private String plateNumber;

    @Column(name = "Model", length = 100)
    private String model;

    @ManyToOne
    @JoinColumn(name = "DriverID", nullable = false)
    @JsonIgnore
    private User driver;

    // Thêm batteryType để xác định loại pin tương thích
    @ManyToOne
    @JoinColumn(name = "BatteryTypeID", nullable = false)
    @JsonIgnore
    private BatteryType batteryType;

    @OneToMany(mappedBy = "vehicle")
    @JsonIgnore
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle")
    @JsonIgnore
    private List<SwapTransaction> swapTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "relatedVehicle")
    @JsonIgnore
    private List<BatteryHistory> batteryHistories = new ArrayList<>();

    // Getter để serialize batteryTypeId
    public Long getBatteryTypeId() {
        return batteryType != null ? batteryType.getId() : null;
    }
}