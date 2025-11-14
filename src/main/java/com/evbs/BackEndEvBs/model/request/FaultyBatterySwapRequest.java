package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FaultyBatterySwapRequest {

    @NotNull(message = "ID xe không thể để trống!")
    private Long vehicleId;

    @NotNull(message = "ID pin thay thế không thể để trống!")
    private Long replacementBatteryId;
}
