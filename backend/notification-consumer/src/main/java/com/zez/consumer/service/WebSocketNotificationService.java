package com.zez.consumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zez.shared.event.PaymentEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Forwards processed payment events to the WebSocket gateway via HTTP.
 *
 * <p>Decorated with a Resilience4j circuit breaker so that a flapping gateway
 * does not cascade failures into the Kafka consumer loop. When the circuit opens,
 * {@link #broadcastFallback} is invoked — the event is already persisted in the DB,
 * so skipping the broadcast is safe; the frontend can recover via the REST API.
 *
 * <p>The RestClient is configured with a 2 s connect timeout and 5 s read timeout
 * to prevent thread-pool starvation under high latency.
 */
@Service
public class WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);
    private static final String CB_NAME = "websocket-gateway";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gateway.url}")
    private String gatewayUrl;

    @Value("${security.internal-api-key:dev-insecure-key-replace-in-prod}")
    private String internalApiKey;

    public WebSocketNotificationService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "broadcastFallback")
    public void broadcast(PaymentEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            restClient.post()
                    .uri(gatewayUrl + "/internal/notify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-API-Key", internalApiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[CONSUMER] Event {} forwarded to WebSocket gateway", event.getEventId());

        } catch (JsonProcessingException e) {
            log.error("[CONSUMER] Failed to serialize event for gateway: {}", e.getMessage());
        }
        // Other exceptions propagate to the circuit breaker
    }

    /**
     * Fallback invoked when the circuit is open or a call fails.
     * The event is already persisted — skipping the broadcast is safe.
     */
    @SuppressWarnings("unused")
    private void broadcastFallback(PaymentEvent event, Throwable cause) {
        log.warn("[CONSUMER] Circuit breaker open — WebSocket broadcast skipped for event {}. " +
                "Cause: {}", event.getEventId(), cause.getMessage());
    }
}


