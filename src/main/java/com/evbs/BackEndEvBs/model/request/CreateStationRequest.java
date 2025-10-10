package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStationRequest {

    @NotEmpty(message = "Name cannot be empty!")
    private String name;

    private String location;

    private Integer capacity;

    private String contactInfo;

    private Double latitude;

    private Double longitude;

    private String status = "Active"; // default
}
