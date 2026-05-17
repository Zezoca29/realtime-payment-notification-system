package com.zez.payment.repository;

import com.zez.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    boolean existsByPaymentId(String paymentId);
    Optional<Payment> findByPaymentId(String paymentId);
    Page<Payment> findByCustomerId(String customerId, Pageable pageable);
}
