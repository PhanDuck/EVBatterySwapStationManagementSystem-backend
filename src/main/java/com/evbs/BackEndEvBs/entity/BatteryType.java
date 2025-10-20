package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "BatteryType")
@Getter
@Setter
public class BatteryType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BatteryTypeID")
    private Long id;

    @NotEmpty(message = "Name cannot be empty!")
    @Column(name = "Name", nullable = false, unique = true, length = 100, columnDefinition = "NVARCHAR(100)")
    private String name;

    @Column(name = "Description", columnDefinition = "NVARCHAR(255)")
    private String description;

    // Các thông số kỹ thuật của loại pin
    @Column(name = "Voltage") // Điện áp (V)
    private Double voltage;

    @Column(name = "Capacity") // Dung lượng (kWh)
    private Double capacity;

    @Column(name = "Weight") // Trọng lượng (kg)
    private Double weight;

    @Column(name = "Dimensions") // Kích thước
    private String dimensions;

    // Relationships
    @OneToMany(mappedBy = "batteryType")
    @JsonIgnore
    private List<Battery> batteries = new ArrayList<>();

    @OneToMany(mappedBy = "batteryType")
    @JsonIgnore
    private List<Vehicle> vehicles = new ArrayList<>();

    @OneToMany(mappedBy = "batteryType")
    @JsonIgnore
    private List<Station> stations = new ArrayList<>();
}