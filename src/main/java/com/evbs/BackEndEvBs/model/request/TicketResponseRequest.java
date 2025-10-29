package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketResponseRequest {

    @NotNull(message = "Ticket ID không được để trống!")
    private Long ticketId;

    @NotEmpty(message = "Message không được để trống!")
    private String message;
}