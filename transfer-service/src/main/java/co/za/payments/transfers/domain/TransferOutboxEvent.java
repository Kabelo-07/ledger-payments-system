package co.za.payments.transfers.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "transfer_outbox_event")
public class TransferOutboxEvent extends AbstractEntity {

    @Column(name = "transfer_id" , nullable = false)
    private UUID transferId;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "number_of_attempts")
    private int numberOfAttempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    public static TransferOutboxEvent instanceOf(UUID transferId, String payload) {
        return new TransferOutboxEvent(transferId, payload, OutboxStatus.PENDING, 0, Instant.now());
    }

    public void markAsProcessed() {
        this.status = OutboxStatus.PROCESSED;
    }

    public void markAsFailed() {
        this.status = OutboxStatus.FAILED;
    }

    public void incrementRetryCount() {
        this.numberOfAttempts++;
    }

    public boolean canRetry(int maxRetries) {
        return this.numberOfAttempts < maxRetries;
    }

}
