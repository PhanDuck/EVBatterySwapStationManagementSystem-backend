package com.evbs.BackEndEvBs.model.request;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.StationInventory;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StationInventoryRequest {

    @NotNull(message = "Station ID cannot be null!")
    private Long stationId;

    @NotNull(message = "Battery ID cannot be null!")
    private Long batteryId;

    @Enumerated(EnumType.STRING)
    private StationInventory.Status status = StationInventory.Status.AVAILABLE;
}