package com.evbs.BackEndEvBs.model.response;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Response cho quick swap preview - hiển thị thông tin pin mới sẽ đổi
 */
@Data
public class QuickSwapPreviewResponse {
    // Thông tin trạm
    private Long stationId;
    private String stationName;
    private String stationLocation;
    
    // Thông tin xe đang chọn
    private Long vehicleId;
    private String vehiclePlateNumber;
    private String vehicleModel;
    
    // Thông tin pin mới (sẽ lên xe)
    private Long newBatteryId;
    private String newBatteryModel;
    private BigDecimal newBatteryChargeLevel;
    private BigDecimal newBatteryHealth;
    
    // Thông tin loại pin
    private String batteryTypeName;
    private Double batteryTypeCapacity;
    
    // Thông tin lượt swap còn lại
    private Integer remainingSwaps;
    
    // Trạng thái
    private boolean canSwap;
    private String message;
}
