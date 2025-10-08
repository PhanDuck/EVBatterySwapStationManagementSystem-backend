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

    private String location;

    @NotNull(message = "Capacity cannot be null!")
    @Positive(message = "Capacity must be positive")
    private Integer capacity;

    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "contactInfo invalid!"
    )
    private String contactInfo;

    private Double latitude;

    private Double longitude;

    private String status = "Active";
}