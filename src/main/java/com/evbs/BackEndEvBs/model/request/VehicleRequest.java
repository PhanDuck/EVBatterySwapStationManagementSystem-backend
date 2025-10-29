package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehicleRequest {

    @NotEmpty(message = "VIN không được để trống!")
    @Size(min = 17, max = 17, message = "VIN phải có chính xác 17 ký tự!")
    private String vin;

    @NotEmpty(message = "Số hiệu tấm không được để trống!")
    @Pattern(
            regexp = "^[0-9]{2}[a-zA-Z]{1,2}[0-9]{5,6}(\\\\.[a-zA-Z]{1,2})?$",
            message = "Định dạng biển số xe máy Việt Nam không hợp lệ! Ví dụ hợp lệ: 29X112345, 51F11234, 30H112350"
    )
    private String plateNumber;

    @NotEmpty(message = "Mẫu xe không được để trống!")
    private String model;

    @NotNull(message = "ID loại pin không thể để trống!")
    private Long batteryTypeId;
}