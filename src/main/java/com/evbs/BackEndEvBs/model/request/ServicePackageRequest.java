package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServicePackageRequest {

    @NotEmpty(message = "Name cannot be empty!")
    private String name;

    @NotEmpty(message = "Description cannot be empty!")
    private String description;

    @NotNull(message = "Price cannot be null!")
    @Positive(message = "Price must be positive!")
    private BigDecimal price;

    @NotNull(message = "Duration cannot be null!")
    @Positive(message = "Duration must be positive!")
    private Integer duration;

    @NotNull(message = "MaxSwaps cannot be null!")
    @Positive(message = "MaxSwaps must be positive!")
    private Integer maxSwaps;
}