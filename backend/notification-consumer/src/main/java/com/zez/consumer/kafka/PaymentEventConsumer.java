package com.zez.consumer.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zez.consumer.service.NotificationProcessingService;
import com.zez.shared.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final NotificationProcessingService processingService;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(NotificationProcessingService processingService, ObjectMapper objectMapper) {
        this.processingService = processingService;
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 5000, multiplier = 2.0, maxDelay = 30000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt",
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${kafka.topic.payment-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        MDC.put("correlationId", correlationId != null ? correlationId : "unknown");
        try {
            log.info("[CONSUMER] Received message from topic={} partition={} offset={}", topic, partition, offset);
            PaymentEvent event = objectMapper.readValue(payload, PaymentEvent.class);
            processingService.process(event);
        } catch (Exception e) {
            log.error("[CONSUMER] Failed to process message from offset={}: {}", offset, e.getMessage());
            throw new RuntimeException("Event processing failed", e);
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "${kafka.topic.payment-events}-dlt", groupId = "${spring.kafka.consumer.group-id}-dlt")
    public void consumeDlt(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.error("[DLT] Event exhausted all retries! topic={} payload={}", topic, payload);
        // TODO: integrate with alerting (PagerDuty, Slack webhook, OpsGenie)
    }
}