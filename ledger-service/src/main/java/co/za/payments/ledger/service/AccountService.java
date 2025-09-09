package co.za.payments.ledger.service;

import co.za.payments.ledger.dto.AccountResponse;
import co.za.payments.ledger.dto.CreateAccountRequest;

import java.util.UUID;

public interface AccountService {

    AccountResponse create(CreateAccountRequest request);

    AccountResponse getAccount(UUID id);
}
