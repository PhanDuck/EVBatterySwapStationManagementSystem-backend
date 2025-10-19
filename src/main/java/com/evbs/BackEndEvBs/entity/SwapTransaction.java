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

    // 🔑 Expose các IDs quan trọng cho JSON response
    @Transient
    public Long getDriverId() {
        return driver != null ? driver.getId() : null;
    }

    @Transient
    public Long getVehicleId() {
        return vehicle != null ? vehicle.getId() : null;
    }

    @Transient
    public Long getStationId() {
        return station != null ? station.getId() : null;
    }

    @Transient
    public Long getStaffId() {
        return staff != null ? staff.getId() : null;
    }

    @ManyToOne
    @JoinColumn(name = "SwapOutBatteryID")
    @JsonIgnore
    private Battery swapOutBattery;

    @ManyToOne
    @JoinColumn(name = "SwapInBatteryID")
    @JsonIgnore
    private Battery swapInBattery;

    // ⭐ SNAPSHOT thông tin pin tại thời điểm swap (để lưu lịch sử không bị mất)
    
    // Thông tin pin lấy RA (swapOut - pin mới lên xe)
    @Column(name = "SwapOutBatteryModel", length = 100, columnDefinition = "NVARCHAR(100)")
    private String swapOutBatteryModel;
    
    @Column(name = "SwapOutBatteryChargeLevel", precision = 5, scale = 2)
    private BigDecimal swapOutBatteryChargeLevel;
    
    @Column(name = "SwapOutBatteryHealth", precision = 5, scale = 2)
    private BigDecimal swapOutBatteryHealth;
    
    // Thông tin pin đem VÀO (swapIn - pin cũ xuống xe)
    @Column(name = "SwapInBatteryModel", length = 100, columnDefinition = "NVARCHAR(100)")
    private String swapInBatteryModel;
    
    @Column(name = "SwapInBatteryChargeLevel", precision = 5, scale = 2)
    private BigDecimal swapInBatteryChargeLevel;
    
    @Column(name = "SwapInBatteryHealth", precision = 5, scale = 2)
    private BigDecimal swapInBatteryHealth;

    // 🔑 Expose Battery IDs cho JSON response
    @Transient
    public Long getSwapOutBatteryId() {
        return swapOutBattery != null ? swapOutBattery.getId() : null;
    }

    @Transient
    public Long getSwapInBatteryId() {
        return swapInBattery != null ? swapInBattery.getId() : null;
    }

    @OneToOne
    @JoinColumn(name = "BookingID")
    @JsonIgnore
    private Booking booking;

    @Transient
    public Long getBookingId() {
        return booking != null ? booking.getId() : null;
    }

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