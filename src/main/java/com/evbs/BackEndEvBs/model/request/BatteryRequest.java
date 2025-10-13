package com.evbs.BackEndEvBs.model.request;

import com.evbs.BackEndEvBs.entity.Battery;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BatteryRequest {

    @NotEmpty(message = "Battery model cannot be empty!")
    private String model;

    @NotNull(message = "Capacity cannot be null!")
    @DecimalMin(value = "0.0", inclusive = false, message = "Capacity must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Capacity must have max 8 integer and 2 fraction digits")
    private BigDecimal capacity;

    @DecimalMin(value = "0.0", message = "State of health cannot be negative")
    @Digits(integer = 3, fraction = 2, message = "State of health must have max 3 integer and 2 fraction digits")
    private BigDecimal stateOfHealth;


    @Enumerated(EnumType.STRING)
    private Battery.Status status = Battery.Status.AVAILABLE;

    private Long currentStationId;
}