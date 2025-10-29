package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServicePackageRequest {

    @NotEmpty(message = "Tên không được để trống!")
    private String name;

    @NotEmpty(message = "Không được để trống phần mô tả!")
    private String description;

    @NotNull(message = "Giá không thể để trống!")
    @Positive(message = "Giá phải là số dương!")
    private BigDecimal price;

    @NotNull(message = "Thời lượng không thể rỗng!")
    @Positive(message = "Thời lượng phải là số dương!")
    private Integer duration;

    @NotNull(message = "MaxSwaps không thể để giá trị null!")
    @Positive(message = "MaxSwaps phải là số dương!")
    private Integer maxSwaps;
}