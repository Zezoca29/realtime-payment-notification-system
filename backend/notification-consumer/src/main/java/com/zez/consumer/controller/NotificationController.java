package com.zez.consumer.controller;

import com.zez.consumer.model.PaymentNotification;
import com.zez.consumer.repository.PaymentNotificationRepository;
import com.zez.shared.event.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final PaymentNotificationRepository repository;

    public NotificationController(PaymentNotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<Page<PaymentNotification>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "processedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<PaymentNotification>> byCustomer(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("processedAt").descending());
        return ResponseEntity.ok(repository.findByCustomerId(customerId, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PaymentNotification>> byStatus(
            @PathVariable PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("processedAt").descending());
        return ResponseEntity.ok(repository.findByStatus(status, pageable));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<PaymentNotification> byEventId(@PathVariable String eventId) {
        return repository.findByEventId(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "notification-consumer"));
    }
}
