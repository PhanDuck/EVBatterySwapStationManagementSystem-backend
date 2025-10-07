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

    @Column(name = "Status", length = 50)
    private String status = "Active";
    
    @OneToMany(mappedBy = "subscription")
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();
}