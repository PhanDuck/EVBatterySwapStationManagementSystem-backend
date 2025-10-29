package com.evbs.BackEndEvBs.model.request;

import com.evbs.BackEndEvBs.entity.Battery;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BatteryUpdateRequest {

    private String model;

    @DecimalMin(value = "0.0", inclusive = false, message = "Dung lượng phải lớn hơn 0  ")
    @Digits(integer = 8, fraction = 2, message = "Dung lượng phải có tối đa 8 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal capacity;

    @DecimalMin(value = "0.0", message = "Tình trạng pin không để ")
    @Digits(integer = 3, fraction = 2, message = "Tình trạng pin có tối đa 3 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal stateOfHealth;

    @Enumerated(EnumType.STRING)
    private Battery.Status status;

    // Thêm các trường mới
    private LocalDate manufactureDate;

    private LocalDate lastMaintenanceDate;

    private Long batteryTypeId;

    private Long currentStationId;
}