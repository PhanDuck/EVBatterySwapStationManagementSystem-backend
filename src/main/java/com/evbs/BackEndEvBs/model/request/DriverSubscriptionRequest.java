package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DriverSubscriptionRequest {
    @NotNull(message = "Package ID cannot be null!")
    private Long packageId;
}