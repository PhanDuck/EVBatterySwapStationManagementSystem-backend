package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Station")
@Getter
@Setter
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StationID")
    private Long id;

    @NotEmpty(message = "Tên không được để trống!")
    @Column(name = "Name", nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
    private String name;

    @Column(name = "Location", length = 255, columnDefinition = "NVARCHAR(255)")
    private String location;

    @Column(name = "Capacity")
    private Integer capacity;

    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Số điện thoại liên lạc không hợp lệ! Phải là số điện thoại theo định dạng tiếng Việt."
    )
    @Column(name = "ContactInfo", length = 12)
    private String contactInfo;

    // Thông tin địa chỉ chi tiết
    @Column(name = "City", length = 100, columnDefinition = "NVARCHAR(100)")
    private String city;

    @Column(name = "District", length = 100, columnDefinition = "NVARCHAR(100)")
    private String district;

    // Tọa độ GPS
    @Column(name = "Latitude")
    private Double latitude;

    @Column(name = "Longitude")
    private Double longitude;

    // Thêm batteryType để xác định loại pin của trạm
    @ManyToOne
    @JoinColumn(name = "BatteryTypeID", nullable = false)
    @JsonIgnore
    private BatteryType batteryType;

    public enum Status {
        ACTIVE, INACTIVE, MAINTENANCE, UNDER_CONSTRUCTION
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private Status status = Status.ACTIVE;

    // Relationships
    @OneToMany(mappedBy = "currentStation")
    @JsonIgnore
    private List<Battery> batteries = new ArrayList<>();

    // StationInventory = KHO TỔNG (không thuộc trạm cụ thể nào)
    // Đã XÓA relationship với Station

    @OneToMany(mappedBy = "station")
    @JsonIgnore
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "station")
    @JsonIgnore
    private List<SwapTransaction> swapTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "station")
    @JsonIgnore
    private List<SupportTicket> supportTickets = new ArrayList<>();

    // Getter để serialize batteryTypeId
    public Long getBatteryTypeId() {
        return batteryType != null ? batteryType.getId() : null;
    }

    // Phương thức tính số pin hiện có trong trạm
    public int getCurrentBatteryCount() {
        return (batteries != null) ? batteries.size() : 0;
    }

    // Phương thức kiểm tra trạm còn chỗ trống không
    public boolean hasAvailableSlot() {
        return getCurrentBatteryCount() < capacity;
    }
}