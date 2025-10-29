package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DriverSubscriptionRequest {
    @NotNull(message = "ID gói không thể rỗng!")
    private Long packageId;
}