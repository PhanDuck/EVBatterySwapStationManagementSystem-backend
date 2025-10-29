package com.evbs.BackEndEvBs.model.request;

import com.evbs.BackEndEvBs.entity.Battery;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BatteryRequest {

    @NotEmpty(message = "model không được để trống!")
    private String model;

    @NotNull(message = "Dung lượng không được để trống!")
    @DecimalMin(value = "0.0", inclusive = false, message = "Dung lượng phải lớn hơn 0")
    @Digits(integer = 8, fraction = 2, message = "Dung lượng phải có tối đa 8 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal capacity;

    @DecimalMin(value = "0.0", message = "State of health không được âm")
    @Digits(integer = 3, fraction = 2, message = "State of health tối đa 3 số nguyên va 2 chữ số thập phân")
    private BigDecimal stateOfHealth;

    @Enumerated(EnumType.STRING)
    private Battery.Status status = Battery.Status.AVAILABLE;

    // Thêm các trường mới
    private LocalDate manufactureDate;

    private LocalDate lastMaintenanceDate;

    @NotNull(message = "batteryTypeId không được để trống!")
    private Long batteryTypeId;

    private Long currentStationId;
}