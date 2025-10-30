package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehicleUpdateRequest {

    @Size(min = 17, max = 17, message = "VIN phải có chính xác 17 ký tự!")
    private String vin;

    @Pattern(
            regexp = "^[0-9]{2}[a-zA-Z]{1,2}[0-9]{5,6}(\\\\.[a-zA-Z]{1,2})?$",
            message = "Định dạng biển số xe máy Việt Nam không hợp lệ! Ví dụ hợp lệ: 29X112345, 51F11234, 30H112350"
    )
    private String plateNumber;

    private String model;

    private Long driverId;

    private Long batteryTypeId;
}