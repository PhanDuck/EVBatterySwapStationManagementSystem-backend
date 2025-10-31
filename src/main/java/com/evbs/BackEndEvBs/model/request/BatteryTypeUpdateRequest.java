package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatteryTypeUpdateRequest {

    private String name;

    private String description;

    private Double voltage;

    private Double capacity;

    private Double weight;

    private String dimensions;
}
