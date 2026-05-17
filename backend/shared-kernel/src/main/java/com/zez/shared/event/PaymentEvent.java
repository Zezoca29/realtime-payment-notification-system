package com.zez.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentEvent {

    private String eventId;
    private String paymentId;
    private String correlationId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String customerId;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int retryCount;

    public PaymentEvent() {}

    public PaymentEvent(String eventId, String paymentId, String correlationId,
                        PaymentStatus status, BigDecimal amount, String currency,
                        String customerId, LocalDateTime timestamp) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.correlationId = correlationId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.customerId = customerId;
        this.timestamp = timestamp;
        this.retryCount = 0;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "eventId='" + eventId + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", status=" + status +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}
