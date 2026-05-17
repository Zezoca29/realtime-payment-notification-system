package com.zez.consumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zez.shared.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gateway.url}")
    private String gatewayUrl;

    public WebSocketNotificationService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public void broadcast(PaymentEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            restClient.post()
                    .uri(gatewayUrl + "/internal/notify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[CONSUMER] Event {} forwarded to WebSocket gateway", event.getEventId());

        } catch (JsonProcessingException e) {
            log.error("[CONSUMER] Failed to serialize event for gateway: {}", e.getMessage());
        } catch (Exception e) {
            // Gateway downtime must not fail the consumer — event is already persisted
            log.warn("[CONSUMER] Could not reach WebSocket gateway (event still persisted): {}", e.getMessage());
        }
    }
}

