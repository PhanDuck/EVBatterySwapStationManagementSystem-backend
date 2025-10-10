package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {

    @NotNull(message = "Vehicle ID cannot be null!")
    private Long vehicleId;

    @NotNull(message = "Station ID cannot be null!")
    private Long stationId;

    @NotNull(message = "Booking time cannot be null!")
    @FutureOrPresent(message = "Booking time cannot be in the past")
    private LocalDateTime bookingTime;
}