package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request để thực hiện đổi pin nhanh
 */
@Data
public class QuickSwapRequest {
    @NotNull(message = "Vui lòng chọn trạm!")
    private Long stationId;
    
    @NotNull(message = "Vui lòng chọn xe!")
    private Long vehicleId;
    
    @NotNull(message = "Vui lòng chọn pin!")
    private Long batteryId;  // Pin đã xem ở Preview, BẮT BUỘC đổi đúng pin này
}
