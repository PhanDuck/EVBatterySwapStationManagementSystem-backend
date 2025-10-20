package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull(message = "Transaction ID cannot be null")
    private Long transactionId;

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotEmpty(message = "Payment method cannot be empty")
    private String paymentMethod;  // CASH, CARD, EWALLET, etc.
}
