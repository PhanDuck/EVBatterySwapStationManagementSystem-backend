package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehicleApproveRequest {

    @NotNull(message = "Battery ID không được để trống!")
    private Long batteryId;
}
