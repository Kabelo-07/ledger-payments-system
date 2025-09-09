package co.za.payments.ledger.exception;

import java.util.UUID;

import static co.za.payments.ledger.config.AppConstants.ACCOUNT_NOT_FOUND_CODE;

public class AccountNotFoundException extends LedgerApplicationException {

    public AccountNotFoundException(UUID accountId) {
        super(ACCOUNT_NOT_FOUND_CODE, "Account with ID: %s does not exist".formatted(accountId));
    }
}
