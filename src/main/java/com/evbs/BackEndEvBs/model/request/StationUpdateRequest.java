package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StationUpdateRequest {

    private String name;

    private String location;

    private Integer capacity;

    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Contact phone number invalid! Must be Vietnamese phone number format."
    )
    private String contactInfo;

    private String city;

    private String district;

    private Double latitude;

    private Double longitude;
}