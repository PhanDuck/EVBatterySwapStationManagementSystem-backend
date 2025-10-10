package com.evbs.BackEndEvBs.model.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServicePackageUpdateRequest {

    private String name;

    private String description;

    private BigDecimal price;

    private Integer duration;

    private Integer maxSwaps;
}