package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "DriverSubscription")
@Getter
@Setter
public class DriverSubscription {

    public enum Status {
        ACTIVE, EXPIRED, CANCELLED, SUSPENDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SubscriptionID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "DriverID", nullable = false)
    @JsonIgnore
    private User driver;

    @ManyToOne
    @JoinColumn(name = "PackageID", nullable = false)
    @JsonIgnore
    private ServicePackage servicePackage;

    @Column(name = "StartDate", nullable = false)
    private LocalDate startDate;

    @Column(name = "EndDate", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private Status status = Status.ACTIVE;

    @Column(name = "RemainingSwaps", nullable = false)
    private Integer remainingSwaps; // Số lần swap còn lại
    
    @OneToMany(mappedBy = "subscription")
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();

    @Transient
    private Long driverId;

    @Transient
    private Long packageId;

    public Long getDriverId() {
        return this.driver != null ? this.driver.getId() : null;
    }

    public Long getPackageId() {
        return this.servicePackage != null ? this.servicePackage.getId() : null;
    }
}