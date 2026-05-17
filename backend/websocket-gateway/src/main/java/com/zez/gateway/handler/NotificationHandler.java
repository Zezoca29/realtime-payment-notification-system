package com.zez.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zez.shared.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
public class NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationHandler.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificationHandler(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Internal endpoint called by notification-consumer (server-to-server only).
     * Receives a PaymentEvent JSON and broadcasts it to all WebSocket subscribers.
     */
    @PostMapping("/notify")
    public ResponseEntity<Void> notify(@RequestBody String payload) {
        try {
            PaymentEvent event = objectMapper.readValue(payload, PaymentEvent.class);
            log.info("[GATEWAY] Broadcasting event {} status={} to WebSocket clients",
                    event.getEventId(), event.getStatus());

            messagingTemplate.convertAndSend("/topic/payments", payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[GATEWAY] Failed to broadcast event: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

