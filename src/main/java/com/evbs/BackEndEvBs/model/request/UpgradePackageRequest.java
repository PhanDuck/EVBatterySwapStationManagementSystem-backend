package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpgradePackageRequest {

    @NotNull(message = "New package ID cannot be null!")
    private Long newPackageId;

    // Optional: Custom redirect URL after MoMo payment
    private String redirectUrl;
}
