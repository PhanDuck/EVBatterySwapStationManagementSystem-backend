package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatteryHistoryRequest {

    @NotNull(message = "không được để trống batteryId!")
    private Long batteryId;

    @NotEmpty(message = "không được để trống eventType!")
    private String eventType;

    private Long stationId;

    private Long vehicleId;

    private Long staffId;
}