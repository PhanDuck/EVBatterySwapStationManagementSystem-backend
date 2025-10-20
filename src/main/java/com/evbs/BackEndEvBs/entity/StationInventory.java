package com.evbs.BackEndEvBs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * StationInventory = KHO TỔNG (Central Warehouse)
 * - Nơi tạo pin mới
 * - Nơi bảo trì tất cả các pin
 * - Pin trong kho: Battery.currentStation = NULL
 * - Pin ở trạm: Battery.currentStation != NULL (KHÔNG có trong StationInventory)
 */
@Entity
@Table(name = "StationInventory")
@Getter
@Setter
public class StationInventory {

    public enum Status {
        AVAILABLE,  // Pin sẵn sàng để gửi đến trạm
        MAINTENANCE // Pin đang bảo trì trong kho
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StationInventoryID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "BatteryID", nullable = false, unique = true)
    @JsonIgnore
    private Battery battery;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private Status status = Status.AVAILABLE;

    @Column(name = "LastUpdate")
    private LocalDateTime lastUpdate = LocalDateTime.now();
}