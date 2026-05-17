package com.zez.consumer.service;

import com.zez.consumer.model.PaymentNotification;
import com.zez.consumer.repository.PaymentNotificationRepository;
import com.zez.shared.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationProcessingService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessingService.class);

    private final PaymentNotificationRepository repository;
    private final WebSocketNotificationService webSocketService;

    public NotificationProcessingService(PaymentNotificationRepository repository,
                                         WebSocketNotificationService webSocketService) {
        this.repository = repository;
        this.webSocketService = webSocketService;
    }

    /**
     * Idempotent processing: checks eventId before persisting.
     * If the same event arrives twice (Kafka at-least-once delivery),
     * we detect the duplicate and skip — ensuring exactly-once semantics.
     */
    @Transactional
    public void process(PaymentEvent event) {
        // --- IDEMPOTENCY CHECK ---
        if (repository.existsByEventId(event.getEventId())) {
            log.warn("[CONSUMER] Duplicate event detected and ignored. eventId={} paymentId={}",
                    event.getEventId(), event.getPaymentId());
            return;
        }

        try {
            PaymentNotification notification = buildNotification(event);
            repository.save(notification);
            log.info("[CONSUMER] Event {} processed successfully for payment {}",
                    event.getEventId(), event.getPaymentId());

            // Forward to WebSocket gateway via HTTP
            webSocketService.broadcast(event);

        } catch (DataIntegrityViolationException e) {
            // Race condition: duplicate insert caught by DB unique constraint — safe to ignore
            log.warn("[CONSUMER] DB-level duplicate caught for eventId={}. Skipping.", event.getEventId());
        }
    }

    private PaymentNotification buildNotification(PaymentEvent event) {
        PaymentNotification n = new PaymentNotification();
        n.setEventId(event.getEventId());
        n.setPaymentId(event.getPaymentId());
        n.setCorrelationId(event.getCorrelationId());
        n.setStatus(event.getStatus());
        n.setAmount(event.getAmount());
        n.setCurrency(event.getCurrency());
        n.setCustomerId(event.getCustomerId());
        n.setEventTimestamp(event.getTimestamp());
        return n;
    }
}
