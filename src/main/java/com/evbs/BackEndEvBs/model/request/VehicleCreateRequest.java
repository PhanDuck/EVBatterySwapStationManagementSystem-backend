package com.evbs.BackEndEvBs.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VehicleCreateRequest {

    @NotEmpty(message = "VIN không được để trống!")
    @Size(min = 17, max = 17, message = "VIN phải có chính xác 17 ký tự!")
    @Schema(description = "Vehicle Identification Number (17 characters)", example = "1HGBH41JXMN109186")
    private String vin;

    @NotEmpty(message = "Số hiệu tấm không được để trống!")
    @Pattern(
            regexp = "^[0-9]{2}[a-zA-Z]{1,2}[0-9]{5,6}(\\.[a-zA-Z]{1,2})?$",
            message = "Định dạng biển số xe máy Việt Nam không hợp lệ! Ví dụ hợp lệ: 29X112345, 51F11234, 30H112350"
    )
    @Schema(description = "Biển số xe", example = "29X112345")
    private String plateNumber;

    @NotEmpty(message = "Mẫu xe không được để trống!")
    @Schema(description = "Model xe", example = "VinFast Klara S")
    private String model;

    @NotNull(message = "ID loại pin không thể để trống!")
    @Schema(description = "Battery Type ID", example = "1")
    private Long batteryTypeId;

    @NotNull(message = "Ảnh giấy đăng ký xe không được để trống!")
    @Schema(description = "Ảnh giấy đăng ký xe (Cà vẹt)", type = "string", format = "binary")
    private MultipartFile registrationImage;
}
