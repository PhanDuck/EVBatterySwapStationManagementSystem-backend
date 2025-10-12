package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BatteryUpdateRequest {

    private String model;

    @DecimalMin(value = "0.0", inclusive = false, message = "Capacity must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Capacity must have max 8 integer and 2 fraction digits")
    private BigDecimal capacity;

    @DecimalMin(value = "0.0", message = "State of health cannot be negative")
    @Digits(integer = 3, fraction = 2, message = "State of health must have max 3 integer and 2 fraction digits")
    private BigDecimal stateOfHealth;

    private com.evbs.BackEndEvBs.entity.Battery.Status status;

    private Long currentStationId;
}