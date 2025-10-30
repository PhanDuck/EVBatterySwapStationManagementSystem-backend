package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StationRequest {

    @NotEmpty(message = "Tên không được để trống!")
    private String name;

    @NotEmpty(message = "Vị trí không được để trống!")
    private String location;

    @NotNull(message = "Sức chứa không thể để trống!")
    @Positive(message = "Công suất phải là số dương!")
    private Integer capacity;

    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Số điện thoại liên lạc không hợp lệ! Phải là số điện thoại theo định dạng tiếng Việt."
    )
    private String contactInfo;

    @NotEmpty(message = "Thành phố không thể trống rỗng!")
    private String city;

    @NotEmpty(message = "Quận không được để trống!")
    private String district;

    private Double latitude;

    private Double longitude;

    @NotNull(message = "ID loại pin không thể để trống!")
    private Long batteryTypeId;
}