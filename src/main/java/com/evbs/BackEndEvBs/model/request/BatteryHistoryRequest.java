package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatteryHistoryRequest {

    @NotNull(message = "Battery ID cannot be null!")
    private Long batteryId;

    @NotEmpty(message = "Event type cannot be empty!")
    private String eventType;

    private Long stationId;

    private Long vehicleId;

    private Long staffId;
}