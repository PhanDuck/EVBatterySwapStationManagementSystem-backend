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

    // Pin hiện tại đang gắn trên xe (nullable - xe có thể không có pin)
    @ManyToOne
    @JoinColumn(name = "CurrentBatteryID")
    @JsonIgnore
    private Battery currentBattery;

    @OneToMany(mappedBy = "vehicle")
    @JsonIgnore
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle")
    @JsonIgnore
    private List<SwapTransaction> swapTransactions = new ArrayList<>();

    // Transient fields để serialize IDs
    @Transient
    private Long driverId;

    @Transient
    private Long batteryTypeId;

    @Transient
    private Long currentBatteryId;

    // Getters để serialize IDs
    public Long getDriverId() {
        return this.driver != null ? this.driver.getId() : null;
    }

    public Long getBatteryTypeId() {
        return this.batteryType != null ? this.batteryType.getId() : null;
    }

    public Long getCurrentBatteryId() {
        return this.currentBattery != null ? this.currentBattery.getId() : null;
    }
}