package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffStationAssignmentRequest {

    @NotNull(message = "ID nhân viên không thể để giá trị null")
    private Long staffId;

    @NotNull(message = "ID trạm không thể là null")
    private Long stationId;
}
