package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {

    @NotNull(message = "Vehicle ID cannot be null!")
    private Long vehicleId;

    @NotNull(message = "Station ID cannot be null!")
    private Long stationId;

    // ĐÃ XÓA TRƯỜNG bookingTime - hệ thống tự động set thời gian 3 tiếng sau
}