package com.zez.payment.repository;

import com.zez.payment.model.OutboxEvent;
import com.zez.payment.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    /**
     * Fetches the next batch of events to publish, ordered by creation time.
     * Limit 50 keeps each scheduler run short; backlog catches up on subsequent runs.
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC LIMIT 50")
    List<OutboxEvent> findPendingBatch();

    long countByStatus(OutboxStatus status);
}
