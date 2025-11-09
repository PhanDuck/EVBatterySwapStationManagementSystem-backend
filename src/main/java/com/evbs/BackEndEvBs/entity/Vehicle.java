package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
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

    @Column(name = "RegistrationImage", length = 500)
    private String registrationImage;

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

    // Soft delete fields
    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private VehicleStatus status = VehicleStatus.ACTIVE;

    @Column(name = "DeletedAt")
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "DeletedBy")
    @JsonIgnore
    private User deletedBy;

    // Enum for vehicle status
    public enum VehicleStatus {
        ACTIVE,
        INACTIVE,
        PENDING
    }

    // Transient fields để serialize IDs
    @Transient
    private Long driverId;

    @Transient
    private Long batteryTypeId;

    @Transient
    private Long currentBatteryId;

    @Transient
    private Long deletedById;

    @Transient
    private Long swapCount;

    @Transient
    private String batteryTypeName;

    @Transient
    private String driverName;

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

    public Long getDeletedById() {
        return this.deletedBy != null ? this.deletedBy.getId() : null;
    }

    @JsonProperty("swapCount")
    public Long getSwapCount() {
        return this.swapCount == null ? 0L : this.swapCount;
    }

    public void setSwapCount(Long swapCount) {
        this.swapCount = swapCount;
    }

    @JsonProperty("batteryTypeName")
    public String getBatteryTypeName() {
        return this.batteryTypeName != null ? this.batteryTypeName : 
               (this.batteryType != null ? this.batteryType.getName() : null);
    }

    public void setBatteryTypeName(String batteryTypeName) {
        this.batteryTypeName = batteryTypeName;
    }

    @JsonProperty("driverName")
    public String getDriverName() {
        return this.driverName != null ? this.driverName :
               (this.driver != null ? this.driver.getFullName() : null);
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
}