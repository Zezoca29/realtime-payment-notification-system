package com.zez.payment.controller;

import com.zez.payment.model.Payment;
import com.zez.payment.model.PaymentRequest;
import com.zez.payment.repository.PaymentRepository;
import com.zez.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentService paymentService, PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        String cid = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        try {
            log.info("[API] Received payment request for customer {} amount {} {}",
                    request.getCustomerId(), request.getAmount(), request.getCurrency());

            Payment payment = paymentService.processPayment(request, cid);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "paymentId", payment.getPaymentId(),
                    "status", payment.getStatus(),
                    "correlationId", cid,
                    "message", "Payment event published to Kafka"
            ));
        } finally {
            MDC.clear();
        }
    }

    @GetMapping
    public ResponseEntity<Page<Payment>> listPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(paymentRepository.findAll(pageable));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "payment-producer"));
    }
}


