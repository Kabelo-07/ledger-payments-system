package co.za.payments.transfers.repository;

import co.za.payments.transfers.domain.OutboxStatus;
import co.za.payments.transfers.domain.TransferOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<TransferOutboxEvent, UUID> {
    List<TransferOutboxEvent> findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(OutboxStatus status, Instant instant);
}
