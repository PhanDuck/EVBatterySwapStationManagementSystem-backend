package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffStationAssignmentRequest {

    @NotNull(message = "Staff ID cannot be null")
    private Long staffId;

    @NotNull(message = "Station ID cannot be null")
    private Long stationId;
}
