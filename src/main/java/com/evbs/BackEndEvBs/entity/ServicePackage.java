package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ServicePackage")
@Getter
@Setter
public class ServicePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PackageID")
    private Long id;

    @NotEmpty(message = "Name cannot be empty!")
    @Column(name = "Name", nullable = false, length = 150,columnDefinition = "NVARCHAR(150)")
    private String name;

    @Column(name = "Description", length = 255, columnDefinition = "NVARCHAR(255)")
    private String description;

    @NotNull(message = "Price cannot be null!")
    @Column(name = "Price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Duration cannot be null!")
    @Column(name = "Duration", nullable = false)
    private Integer duration; // days

    @NotNull(message = "MaxSwaps cannot be null!")
    @Column(name = "MaxSwaps", nullable = false)
    private Integer maxSwaps;
    
    @OneToMany(mappedBy = "servicePackage")
    @JsonIgnore
    private List<DriverSubscription> subscriptions = new ArrayList<>();
}