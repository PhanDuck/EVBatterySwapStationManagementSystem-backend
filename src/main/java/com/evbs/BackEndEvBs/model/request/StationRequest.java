package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StationRequest {

    @NotEmpty(message = "Name cannot be empty!")
    private String name;

    private String location;

    @NotNull(message = "Capacity cannot be null!")
    @Positive(message = "Capacity must be positive")
    private Integer capacity;

    private String contactInfo;

    private String status = "Active";
}