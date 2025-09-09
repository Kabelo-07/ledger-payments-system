package co.za.payments.ledger.exception;

import java.util.UUID;

import static co.za.payments.ledger.config.AppConstants.INSUFFICIENT_BAL;

public class InsufficientAccountBalanceException extends LedgerApplicationException {

    public InsufficientAccountBalanceException(UUID accountId) {
        super(INSUFFICIENT_BAL, "Insufficient funds in account %s".formatted(accountId));
    }
}
