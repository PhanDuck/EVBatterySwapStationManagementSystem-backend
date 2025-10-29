package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {

    @NotNull(message = "ID xe không thể rỗng!")
    private Long vehicleId;

        @NotNull(message = "ID trạm không thể để giá trị null!")
    private Long stationId;

    // ĐÃ XÓA TRƯỜNG bookingTime - hệ thống tự động set thời gian 3 tiếng sau
}