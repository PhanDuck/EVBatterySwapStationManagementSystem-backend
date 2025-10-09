package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Battery")
@Getter
@Setter
public class Battery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BatteryID")
    private Long id;

    @Column(name = "Model", length = 100,columnDefinition = "NVARCHAR(100)")
    private String model;

    @Column(name = "Capacity", precision = 10, scale = 2)
    private BigDecimal capacity;

    @Column(name = "StateOfHealth", precision = 5, scale = 2)
    private BigDecimal stateOfHealth;

    @Column(name = "Status", length = 50)
    private String status = "Available";

    @ManyToOne
    @JoinColumn(name = "CurrentStationID")
    @JsonIgnore
    private Station currentStation;
    
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
}