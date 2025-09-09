package co.za.payments.ledger.service.impl;

import co.za.payments.ledger.domain.Account;
import co.za.payments.ledger.dto.AccountResponse;
import co.za.payments.ledger.dto.CreateAccountRequest;
import co.za.payments.ledger.exception.AccountNotFoundException;
import co.za.payments.ledger.repository.AccountRepository;
import co.za.payments.ledger.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repository;

    @Override
    public AccountResponse create(CreateAccountRequest request) {
        log.info("Creating account, request: {}", request);

        var account = Account.instanceOf(request.balance());
        account = repository.save(account);

        log.info("Account, created, accountId: {}, initialBalance: {}", account.getId(), account.getBalance());
        return mapResponse(account);
    }

    @Override
    public AccountResponse getAccount(UUID id) {
        log.info("Retrieving account with accountId: {}", id);

        return repository.findById(id)
                .map(this::mapResponse)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private AccountResponse mapResponse(Account account) {
        return new AccountResponse(account.getId(), account.getAccountNumber(),
                account.getBalance(), account.getCreatedAt());
    }
}
