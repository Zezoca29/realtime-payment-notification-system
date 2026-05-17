package com.zez.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zez.consumer.repository.PaymentNotificationRepository;
import com.zez.shared.event.PaymentEvent;
import com.zez.shared.event.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class IdempotencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payments_db")
            .withUsername("payments_user")
            .withPassword("payments_pass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("gateway.url", () -> "http://localhost:9999"); // gateway not running in test
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentNotificationRepository repository;

    @Test
    void shouldProcessEventOnlyOnce_whenDuplicateArrives() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String eventId = UUID.randomUUID().toString();
        PaymentEvent event = new PaymentEvent(
                eventId, UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                PaymentStatus.APPROVED, new BigDecimal("200.00"), "BRL",
                "customer-test", LocalDateTime.now()
        );

        String payload = mapper.writeValueAsString(event);

        // Publish the same event TWICE (simulating Kafka redelivery)
        kafkaTemplate.send("payment-events", event.getPaymentId(), payload);
        kafkaTemplate.send("payment-events", event.getPaymentId(), payload);

        // Wait for consumer to process
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long count = repository.findAll().stream()
                            .filter(n -> n.getEventId().equals(eventId))
                            .count();
                    // Must be exactly 1 despite 2 deliveries
                    assertThat(count).isEqualTo(1);
                });
    }
}
