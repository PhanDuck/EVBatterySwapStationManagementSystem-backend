package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StationRequest {

    @NotEmpty(message = "Name cannot be empty!")
    private String name;

    private String location;

    @NotNull(message = "Capacity cannot be null!")
    private Integer capacity;

    private String contactInfo;

    private Double latitude;

    private Double longitude;
}