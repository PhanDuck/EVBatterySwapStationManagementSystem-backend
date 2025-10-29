package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SupportTicketRequest {

    private Long stationId;

    @NotEmpty(message = "Chủ đề không được để trống!")
    private String subject;

    @NotEmpty(message = "Mô tả không được để trống!")
    private String description;
}