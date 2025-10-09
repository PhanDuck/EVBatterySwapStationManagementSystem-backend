package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StationInventoryRequest {

    @NotNull(message = "Station ID cannot be null!")
    private Long stationId;

    @NotNull(message = "Battery ID cannot be null!")
    private Long batteryId;

    private String status = "Available";
}