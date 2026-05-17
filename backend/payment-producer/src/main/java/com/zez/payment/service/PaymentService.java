package com.zez.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zez.payment.model.OutboxEvent;
import com.zez.payment.model.Payment;
import com.zez.payment.model.PaymentRequest;
import com.zez.payment.repository.OutboxEventRepository;
import com.zez.payment.repository.PaymentRepository;
import com.zez.shared.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxEventRepository outboxRepository,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists the Payment and an OutboxEvent atomically in a single DB transaction.
     *
     * <p>The Outbox relay scheduler ({@link com.zez.payment.scheduler.OutboxEventPublisher})
     * will pick up the PENDING row and publish it to Kafka asynchronously.
     * This eliminates the dual-write problem: if Kafka is down, the payment is still
     * saved and will be delivered once Kafka recovers — without data loss.
     */
    @Transactional
    public Payment processPayment(PaymentRequest request, String correlationId) {
        String paymentId = UUID.randomUUID().toString();
        String eventId   = UUID.randomUUID().toString();

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setStatus(request.getStatus());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency().toUpperCase());
        payment.setCustomerId(request.getCustomerId());
        Payment saved = paymentRepository.save(payment);

        log.info("[SERVICE] Payment {} persisted with status {}", paymentId, request.getStatus());

        PaymentEvent event = new PaymentEvent(
                eventId,
                paymentId,
                correlationId,
                request.getStatus(),
                request.getAmount(),
                request.getCurrency().toUpperCase(),
                request.getCustomerId(),
                LocalDateTime.now()
        );

        // Write to outbox in the same transaction — Kafka publish happens asynchronously
        try {
            OutboxEvent outbox = new OutboxEvent();
            outbox.setEventId(eventId);
            outbox.setPaymentId(paymentId);
            outbox.setCorrelationId(correlationId);
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outboxRepository.save(outbox);
            log.debug("[SERVICE] Outbox event {} queued for relay", eventId);
        } catch (JsonProcessingException e) {
            // Serialization failure is a programming error — fail fast
            throw new RuntimeException("Failed to serialize PaymentEvent for outbox", e);
        }

        return saved;
    }
}


