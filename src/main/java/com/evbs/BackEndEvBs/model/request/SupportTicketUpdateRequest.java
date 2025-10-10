package com.evbs.BackEndEvBs.model.request;

import lombok.Data;

@Data
public class SupportTicketUpdateRequest {

    private Long stationId;

    private String subject;

    private String description;

    private String status;
}