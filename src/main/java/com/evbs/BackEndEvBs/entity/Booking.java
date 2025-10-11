package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Booking")
@Getter
@Setter
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookingID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "DriverID", nullable = false)
    @JsonIgnore
    private User driver;

    @Transient
    private Long driverId;

    @ManyToOne
    @JoinColumn(name = "VehicleID", nullable = false)
    @JsonIgnore
    private Vehicle vehicle;

    @Transient
    private Long vehicleId;

    @ManyToOne
    @JoinColumn(name = "StationID", nullable = false)
    @JsonIgnore
    private Station station;

    @Transient
    private Long stationId;

    @Column(name = "BookingTime", nullable = false)
    private LocalDateTime bookingTime;

    @Column(name = "Status", length = 50)
    private String status = "Pending";

    public Long getDriverId() {
        return this.driver != null ? this.driver.getId() : null;
    }

    public Long getVehicleId() {
        return this.vehicle != null ? this.vehicle.getId() : null;
    }

    public Long getStationId() {
        return this.station != null ? this.station.getId() : null;
    }
}