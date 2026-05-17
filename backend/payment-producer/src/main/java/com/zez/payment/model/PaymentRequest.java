package com.zez.payment.model;

import com.zez.shared.event.PaymentStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class PaymentRequest {

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "invalid amount format")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be 3 characters (ISO 4217)")
    private String currency;

    @NotNull(message = "status is required")
    private PaymentStatus status;

    public PaymentRequest() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
}
