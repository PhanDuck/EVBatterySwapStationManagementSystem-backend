package com.evbs.BackEndEvBs.model.request;

import lombok.Data;

@Data
public class VehicleUpdateRequest {

    // Không có validation @Size vì update không bắt buộc
    private String vin;

    // Không có validation @Pattern vì update không bắt buộc
    private String plateNumber;

    private String model;

    private Long driverId;

    private Long batteryTypeId;

    private String status; // ACTIVE, INACTIVE, PENDING
}