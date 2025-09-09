package co.za.payments.transfers.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer extends AbstractEntity {

    @Column(name = "from_account_id" , nullable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id" , nullable = false)
    private UUID toAccountId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    public static Transfer instanceOf(UUID fromAccountId, BigDecimal amount, UUID toAccountId) {
        return new Transfer(fromAccountId, toAccountId, amount, TransferStatus.PROCESSING);
    }

    public void markAsCompleted() {
        this.status = TransferStatus.COMPLETED;
    }

    public void markAsFailed() {
        this.status = TransferStatus.FAILED;
    }
}
