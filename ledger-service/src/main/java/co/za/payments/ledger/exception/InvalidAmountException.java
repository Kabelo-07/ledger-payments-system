package co.za.payments.ledger.exception;

import co.za.payments.ledger.domain.EntryType;

import java.math.BigDecimal;

import static co.za.payments.ledger.config.AppConstants.INVALID_AMT_CODE;

public class InvalidAmountException extends LedgerApplicationException {

    public InvalidAmountException(EntryType type, BigDecimal amount) {
        super(INVALID_AMT_CODE, "%s amount of %.2f is incorrect, must be positive".formatted(type.getDescription(), amount));
    }
}
