package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SwapTransactionRequest {

    @NotNull(message = "Vehicle ID cannot be null!")
    private Long vehicleId;

    @NotNull(message = "Station ID cannot be null!")
    private Long stationId;

    @NotNull(message = "Staff ID cannot be null!")
    private Long staffId;

    private Long swapOutBatteryId;

    private Long swapInBatteryId;

    private BigDecimal cost;
}