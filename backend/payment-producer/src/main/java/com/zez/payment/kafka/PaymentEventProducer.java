package com.zez.payment.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zez.shared.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.payment-events}")
    private String paymentEventsTopic;

    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(PaymentEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            var message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, paymentEventsTopic)
                    .setHeader(KafkaHeaders.KEY, event.getPaymentId())
                    .setHeader("X-Correlation-ID", event.getCorrelationId())
                    .setHeader("X-Event-ID", event.getEventId())
                    .build();

            kafkaTemplate.send(message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[PRODUCER] Failed to publish event {} for payment {}: {}",
                                    event.getEventId(), event.getPaymentId(), ex.getMessage());
                        } else {
                            log.info("[PRODUCER] Published event {} for payment {} to partition {} offset {}",
                                    event.getEventId(), event.getPaymentId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("[PRODUCER] Serialization error for event {}: {}", event.getEventId(), e.getMessage());
            throw new RuntimeException("Failed to serialize payment event", e);
        }
    }
}