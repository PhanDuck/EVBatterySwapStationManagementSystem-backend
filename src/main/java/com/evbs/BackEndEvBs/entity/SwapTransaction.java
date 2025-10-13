package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SwapTransaction")
@Getter
@Setter
public class SwapTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "DriverID", nullable = false)
    @JsonIgnore
    private User driver;

    @ManyToOne
    @JoinColumn(name = "VehicleID", nullable = false)
    @JsonIgnore
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "StationID", nullable = false)
    @JsonIgnore
    private Station station;

    @ManyToOne
    @JoinColumn(name = "StaffID", nullable = false)
    @JsonIgnore
    private User staff;

    @ManyToOne
    @JoinColumn(name = "SwapOutBatteryID")
    @JsonIgnore
    private Battery swapOutBattery;

    @ManyToOne
    @JoinColumn(name = "SwapInBatteryID")
    @JsonIgnore
    private Battery swapInBattery;

    @Column(name = "StartTime")
    private LocalDateTime startTime;

    @Column(name = "EndTime")
    private LocalDateTime endTime;

    @Column(name = "Cost", precision = 12, scale = 2)
    private BigDecimal cost;

    public enum Status {
        PENDING_PAYMENT, PAID, IN_PROGRESS, COMPLETED, CANCELLED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private Status status = Status.PENDING_PAYMENT;
    
    @OneToMany(mappedBy = "transaction")
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();
}