package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatteryTypeRequest {

    @NotEmpty(message = "Name cannot be empty!")
    private String name;

    private String description;

    @NotNull(message = "Voltage cannot be null!")
    private Double voltage;

    @NotNull(message = "Capacity cannot be null!")
    private Double capacity;

    @NotNull(message = "Weight cannot be null!")
    private Double weight;

    private String dimensions;
}