package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServicePackageRequest {

    private String name;

    private String description;

    private BigDecimal price;

    private Integer duration;

    private Integer maxSwaps;
}