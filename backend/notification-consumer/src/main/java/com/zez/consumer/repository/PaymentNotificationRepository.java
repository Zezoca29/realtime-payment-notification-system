package com.zez.consumer.repository;

import com.zez.consumer.model.PaymentNotification;
import com.zez.shared.event.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentNotificationRepository extends JpaRepository<PaymentNotification, String> {
    boolean existsByEventId(String eventId);
    Optional<PaymentNotification> findByEventId(String eventId);
    Page<PaymentNotification> findByCustomerId(String customerId, Pageable pageable);
    Page<PaymentNotification> findByStatus(PaymentStatus status, Pageable pageable);
}

