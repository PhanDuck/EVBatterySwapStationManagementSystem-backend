package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehicleUpdateRequest {

    @Size(min = 17, max = 17, message = "VIN must be exactly 17 characters!")
    private String vin;

    @Pattern(
            regexp = "^[0-9]{2}[a-zA-Z]{1,2}[0-9]{5,6}(\\\\.[a-zA-Z]{1,2})?$",
            message = "Invalid Vietnamese motorcycle plate format! Valid examples: 29X112345, 51F11234, 30H112350"
    )
    private String plateNumber;

    private String model;

    private Long driverId;
}