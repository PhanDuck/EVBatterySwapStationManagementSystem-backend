package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatteryTypeRequest {

    @NotEmpty(message = "Name không được để trống!")
    private String name;

    private String description;

    @NotNull(message = "Voltage không được để trống!")
    private Double voltage;

    @NotNull(message = "Capacity không được để trống!")
    private Double capacity;

    @NotNull(message = "Weight không được để trống!")
    private Double weight;

    private String dimensions;
}