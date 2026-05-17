package com.zez.payment.service;

import com.zez.payment.kafka.PaymentEventProducer;
import com.zez.payment.model.Payment;
import com.zez.payment.model.PaymentRequest;
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
    private final PaymentEventProducer producer;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventProducer producer) {
        this.paymentRepository = paymentRepository;
        this.producer = producer;
    }

    @Transactional
    public Payment processPayment(PaymentRequest request, String correlationId) {
        String paymentId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();

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

        producer.publish(event);
        return saved;
    }
}

