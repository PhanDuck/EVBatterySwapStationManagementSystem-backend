package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StationRequest {

    @NotEmpty(message = "Name cannot be empty!")
    private String name;

    @NotEmpty(message = "Location cannot be empty!")
    private String location;

    @NotNull(message = "Capacity cannot be null!")
    @Positive(message = "Capacity must be positive!")
    private Integer capacity;

    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Contact phone number invalid! Must be Vietnamese phone number format."
    )
    private String contactInfo;

    @NotEmpty(message = "City cannot be empty!")
    private String city;

    @NotEmpty(message = "District cannot be empty!")
    private String district;

    private Double latitude;

    private Double longitude;

    @NotNull(message = "Battery type ID cannot be null!")
    private Long batteryTypeId;
}