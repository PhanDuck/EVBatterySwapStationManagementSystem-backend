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
        AVAILABLE, IN_USE, CHARGING, MAINTENANCE, DAMAGED
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

    // ✅ State of Charge (SOC) - Mức pin hiện tại (0-100%)
    @Column(name = "ChargeLevel", precision = 5, scale = 2)
    private BigDecimal chargeLevel = BigDecimal.valueOf(100.0);  // Default 100% khi mới

    // ✅ Thời điểm bắt đầu sạc (để tính thời gian sạc)
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
    private List<BatteryHistory> batteryHistories = new ArrayList<>();

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