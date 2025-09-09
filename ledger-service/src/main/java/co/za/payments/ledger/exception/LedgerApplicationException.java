package co.za.payments.ledger.exception;

import lombok.Getter;

@Getter
public abstract class LedgerApplicationException extends RuntimeException {

    private final String code;

    protected LedgerApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }
}
