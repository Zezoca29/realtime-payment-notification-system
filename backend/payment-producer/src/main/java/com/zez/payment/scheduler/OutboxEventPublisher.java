package com.zez.payment.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zez.payment.kafka.PaymentEventProducer;
import com.zez.payment.model.OutboxEvent;
import com.zez.payment.model.OutboxStatus;
import com.zez.payment.repository.OutboxEventRepository;
import com.zez.shared.event.PaymentEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox relay: polls PENDING rows and publishes them to Kafka.
 *
 * <p>Runs every second with a fixed delay (not fixed rate) so concurrent executions
 * are impossible. The @Transactional boundary ensures status updates are durable
 * before the next tick.
 *
 * <p>Failure policy: up to 5 retries, then marks FAILED and emits a metric alert.
 */
@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxRepository;
    private final PaymentEventProducer producer;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public OutboxEventPublisher(OutboxEventRepository outboxRepository,
                                PaymentEventProducer producer,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.producer = producer;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outboxRepository.findPendingBatch();
        if (pending.isEmpty()) return;

        log.debug("[OUTBOX] Relaying {} pending event(s)", pending.size());

        for (OutboxEvent outbox : pending) {
            try {
                PaymentEvent event = objectMapper.readValue(outbox.getPayload(), PaymentEvent.class);
                producer.publish(event);

                outbox.setStatus(OutboxStatus.SENT);
                outbox.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(outbox);

                meterRegistry.counter("payment.outbox.sent").increment();
                log.info("[OUTBOX] Event {} relayed to Kafka successfully", outbox.getEventId());

            } catch (Exception e) {
                int attempts = outbox.getRetryCount() + 1;
                outbox.setRetryCount(attempts);

                if (attempts >= MAX_RETRIES) {
                    outbox.setStatus(OutboxStatus.FAILED);
                    meterRegistry.counter("payment.outbox.failed").increment();
                    log.error("[OUTBOX] Event {} permanently failed after {} attempts — manual intervention required",
                            outbox.getEventId(), attempts);
                } else {
                    log.warn("[OUTBOX] Event {} failed (attempt {}/{}): {}",
                            outbox.getEventId(), attempts, MAX_RETRIES, e.getMessage());
                }
                outboxRepository.save(outbox);
            }
        }

        // Expose pending gauge for Grafana alert
        meterRegistry.gauge("payment.outbox.pending",
                outboxRepository.countByStatus(OutboxStatus.PENDING));
    }
}
