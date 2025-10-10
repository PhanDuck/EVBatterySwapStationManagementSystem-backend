package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketResponseRequest {

    @NotNull(message = "Ticket ID cannot be null!")
    private Long ticketId;

    @NotEmpty(message = "Message cannot be empty!")
    private String message;
}