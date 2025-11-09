package com.evbs.BackEndEvBs.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request body for rejecting a vehicle registration")
public class VehicleRejectRequest {

    @Schema(description = "Reason for rejecting the vehicle (optional)", example = "Giấy đăng ký xe không rõ ràng")
    private String rejectionReason;
}
