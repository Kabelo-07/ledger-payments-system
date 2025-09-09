package co.za.payments.ledger.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ledger_entry", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"transfer_id", "type"})
})
public class LedgerEntry extends AbstractEntity {

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false, updatable = false, precision = 18, scale = 2)
    @Min(1)
    private BigDecimal amount;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EntryType type;

    public static LedgerEntry debit(UUID transferId, UUID accountId, BigDecimal amount) {
        return new LedgerEntry(transferId, accountId, amount, EntryType.DEBIT);
    }

    public static LedgerEntry credit(UUID transferId, UUID accountId, BigDecimal amount) {
        return new LedgerEntry(transferId, accountId, amount, EntryType.CREDIT);
    }

    public boolean isDebit() {
        return this.type.equals(EntryType.DEBIT);
    }

    public boolean isCredit() {
        return this.type.equals(EntryType.CREDIT);
    }
}
