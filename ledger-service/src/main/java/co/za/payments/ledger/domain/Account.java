package co.za.payments.ledger.domain;


import co.za.payments.ledger.exception.InsufficientAccountBalanceException;
import co.za.payments.ledger.exception.InvalidAmountException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import util.AccountNumberGenerator;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "accounts")
public class Account extends VersionedEntity {

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    public static Account instanceOf(BigDecimal balance) {
        return new Account(balance, AccountNumberGenerator.generate());
    }

    private Account(BigDecimal balance, String accountNumber) {
        this.balance = balance;
        this.accountNumber = accountNumber;
    }

    public void credit(BigDecimal amount) {
        this.validateAmount(amount, EntryType.CREDIT);
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        validateAmount(amount, EntryType.DEBIT);

        if (hasInsufficientFunds(amount)) {
            throw new InsufficientAccountBalanceException(this.getId());
        }

        this.balance = this.balance.subtract(amount);
    }

    private boolean hasInsufficientFunds(BigDecimal amount) {
        return this.balance.compareTo(amount) < 0;
    }

    private void validateAmount(BigDecimal amount, EntryType type) {
        if (null == amount || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(type, amount);
        }
    }

}
