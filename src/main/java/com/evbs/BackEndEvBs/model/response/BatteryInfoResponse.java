package com.evbs.BackEndEvBs.model.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BatteryInfoResponse {
    private String confirmationCode;
    private String driverName;
    private String vehiclePlate;
    private String stationName;

    // Thông tin pin
    private Long batteryId;
    private String model;
    private BigDecimal chargeLevel;
    private BigDecimal stateOfHealth;
    private String status;
    private Integer usageCount;
    private String batteryType;

    private String message;
    private String batteryRole; // "OLD" hoặc "NEW"
}   