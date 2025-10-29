package com.evbs.BackEndEvBs.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull(message = "ID giao dịch không thể rỗng")
    private Long transactionId;

    @NotNull(message = "Số tiền không thể là nulll")
    @Positive(message = "Số tiền phải là số dương")
    private BigDecimal amount;

    @NotEmpty(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;  // CASH, CARD, EWALLET, etc.
}
