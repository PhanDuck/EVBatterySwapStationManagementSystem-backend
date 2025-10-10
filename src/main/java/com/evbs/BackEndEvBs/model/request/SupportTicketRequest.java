package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SupportTicketRequest {

    private Long stationId;

    @NotEmpty(message = "Subject cannot be empty!")
    private String subject;

    @NotEmpty(message = "Description cannot be empty!")
    private String description;
}