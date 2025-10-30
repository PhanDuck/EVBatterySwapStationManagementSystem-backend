package com.evbs.BackEndEvBs.model.request;

import com.evbs.BackEndEvBs.entity.Battery;
import com.evbs.BackEndEvBs.entity.StationInventory;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StationInventoryRequest {

    @NotNull(message = "ID trạm không thể để giá trị null!")
    private Long stationId;

    @NotNull(message = "ID pin không thể để giá trị null!")
    private Long batteryId;

    @Enumerated(EnumType.STRING)
    private StationInventory.Status status = StationInventory.Status.AVAILABLE;
}