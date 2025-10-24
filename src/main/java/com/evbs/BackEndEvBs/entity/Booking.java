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

    public enum Status {
        PENDING, CONFIRMED, COMPLETED, CANCELLED
    }

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

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ConfirmationCode", unique = true, length = 20, nullable = true)
    private String confirmationCode;  // Mã xác nhận (null khi PENDING, có giá trị khi CONFIRMED)

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private Status status = Status.PENDING;

    @ManyToOne
    @JoinColumn(name = "ReservedBatteryID")
    @JsonIgnore
    private Battery reservedBattery;

    @Transient
    private Long reservedBatteryId;

    @Column(name = "ReservationExpiry")
    private LocalDateTime reservationExpiry;

    @ManyToOne
    @JoinColumn(name = "ConfirmedBy", nullable = true)
    @JsonIgnore
    private User confirmedBy;  // Staff/Admin đã confirm booking (null nếu chưa confirm)

    @Transient
    private Long confirmedById;

    @OneToOne(mappedBy = "booking")
    @JsonIgnore
    private SwapTransaction swapTransaction;

    @Transient
    private Long swapTransactionId;

    // Expose IDs
    public Long getDriverId() {
        return this.driver != null ? this.driver.getId() : null;
    }

    public Long getVehicleId() {
        return this.vehicle != null ? this.vehicle.getId() : null;
    }

    public Long getStationId() {
        return this.station != null ? this.station.getId() : null;
    }

    public Long getConfirmedById() {
        return this.confirmedBy != null ? this.confirmedBy.getId() : null;
    }

    public Long getReservedBatteryId() {
        return this.reservedBattery != null ? this.reservedBattery.getId() : null;
    }

    public Long getSwapTransactionId() {
        return this.swapTransaction != null ? this.swapTransaction.getId() : null;
    }

    // Expose Battery Info từ SwapTransaction
    @Transient
    public Long getSwapOutBatteryId() {
        return this.swapTransaction != null && this.swapTransaction.getSwapOutBattery() != null 
            ? this.swapTransaction.getSwapOutBattery().getId() 
            : null;
    }

    @Transient
    public Long getSwapInBatteryId() {
        return this.swapTransaction != null && this.swapTransaction.getSwapInBattery() != null 
            ? this.swapTransaction.getSwapInBattery().getId() 
            : null;
    }

    @Transient
    public String getSwapOutBatteryModel() {
        return this.swapTransaction != null 
            ? this.swapTransaction.getSwapOutBatteryModel() 
            : null;
    }

    @Transient
    public String getSwapInBatteryModel() {
        return this.swapTransaction != null 
            ? this.swapTransaction.getSwapInBatteryModel() 
            : null;
    }
}