package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SwapTransactionRequest {

    private Long bookingId; // Optional: Link to booking if exists

    @NotNull(message = "ID xe không thể để trống!")
    private Long vehicleId;

    @NotNull(message = "ID trạm không thể để giá trị null!")
    private Long stationId;

    @NotNull(message = "ID nhân viên không thể để trống!")
    private Long staffId;

    private Long swapOutBatteryId;

    private Long swapInBatteryId;

    private BigDecimal cost;
}