package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Battery")
@Getter
@Setter
public class Battery {

    public enum Status {
        AVAILABLE,    // Pin sẵn sàng để đổi
        PENDING,      // Pin đang được giữ cho booking (reserved)
        IN_USE,       // Pin đang được sử dụng bởi tài xế
        CHARGING,     // Pin đang sạc
        MAINTENANCE,
        RETIRED       // Pin hỏng không thể tái sử dụng

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BatteryID")
    private Long id;

    @Column(name = "Model", length = 100, columnDefinition = "NVARCHAR(100)")
    private String model;

    @Column(name = "Capacity", precision = 10, scale = 2)
    private BigDecimal capacity;

    @Column(name = "StateOfHealth", precision = 5, scale = 2)
    private BigDecimal stateOfHealth;

    //  State of Charge (SOC) - Mức pin hiện tại (0-100%)
    @Column(name = "ChargeLevel", precision = 5, scale = 2)
    private BigDecimal chargeLevel = BigDecimal.valueOf(100.0);  // Default 100% khi mới

    //  Thời điểm bắt đầu sạc (để tính thời gian sạc)
    @Column(name = "LastChargedTime")
    private LocalDateTime lastChargedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private Status status = Status.AVAILABLE;

    // Thêm các trường mới
    @Column(name = "ManufactureDate")
    private LocalDate manufactureDate;

    @Column(name = "UsageCount")
    private Integer usageCount = 0;

    @Column(name = "LastMaintenanceDate")
    private LocalDate lastMaintenanceDate;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ========== BOOKING RESERVATION FIELDS ==========
    // Khi pin được reserve cho booking (status = PENDING)
    @ManyToOne
    @JoinColumn(name = "ReservedForBookingID")
    @JsonIgnore
    private Booking reservedForBooking;

    @Column(name = "ReservationExpiry")
    private LocalDateTime reservationExpiry;  // Hết hạn sau 3 giờ

    @ManyToOne
    @JoinColumn(name = "CurrentStationID")
    @JsonIgnore
    private Station currentStation;

    @ManyToOne
    @JoinColumn(name = "BatteryTypeID", nullable = false)
    @JsonIgnore
    private BatteryType batteryType;

    @OneToMany(mappedBy = "battery")
    @JsonIgnore
    private List<StationInventory> stationInventories = new ArrayList<>();

    @OneToMany(mappedBy = "swapInBattery")
    @JsonIgnore
    private List<SwapTransaction> swapInTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "swapOutBattery")
    @JsonIgnore
    private List<SwapTransaction> swapOutTransactions = new ArrayList<>();

    // Getter để serialize currentStationId thay vì toàn bộ Station object
    @JsonProperty("currentStation")
    public Long getCurrentStationId() {
        return currentStation != null ? currentStation.getId() : null;
    }

    // Getter để serialize batteryTypeId
    @JsonProperty("batteryTypeId")
    public Long getBatteryTypeId() {
        return batteryType != null ? batteryType.getId() : null;
    }

    // Phương thức tăng số lần sử dụng
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null) ? 1 : this.usageCount + 1;
    }
}