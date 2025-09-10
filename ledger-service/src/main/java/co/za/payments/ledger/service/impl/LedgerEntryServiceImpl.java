package co.za.payments.ledger.service.impl;

import co.za.payments.ledger.domain.Account;
import co.za.payments.ledger.domain.LedgerEntry;
import co.za.payments.ledger.dto.LedgerEntryDto;
import co.za.payments.ledger.dto.LedgerTransferResponse;
import co.za.payments.ledger.dto.TransferRequest;
import co.za.payments.ledger.exception.AccountNotFoundException;
import co.za.payments.ledger.repository.AccountRepository;
import co.za.payments.ledger.repository.LedgerEntryRepository;
import co.za.payments.ledger.service.LedgerService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerEntryServiceImpl implements LedgerService {

    private final LedgerEntryRepository ledgerRepository;
    private final AccountRepository accountRepository;

    @Retryable(
            retryFor = {OptimisticLockException.class, OptimisticLockingFailureException.class},
            backoff = @Backoff(delay = 50, multiplier = 2, random = true),
            maxAttempts = 4)
    @Override
    @Transactional
    public LedgerTransferResponse createEntry(TransferRequest request) {
        var existingTransfers = ledgerRepository.findByTransferId(request.transferId());

        if (!existingTransfers.isEmpty()) {
            log.info("Ledger entry exists for transferId: [{}]. Returning existing transfer", request.transferId());

            return mapResponse(existingTransfers);
        }

        log.info("Creating ledger entry, request {}", request);

        var fromAccount = retrieveAccount(request.fromAccountId());
        var toAccount = retrieveAccount(request.toAccountId());

        var ledgerEntries = transfer(request.transferId(), request.amount(), fromAccount, toAccount);

        ledgerEntries = ledgerRepository.saveAll(ledgerEntries);

        var response = mapResponse(ledgerEntries);

        log.info("Ledger entry created for transferId: [{}], amount: [{}]", response.transferId(), response.debitEntry().amount());
        return response;
    }

    private Account retrieveAccount(UUID accountId) {
        log.info("Retrieving account with accountId: [{}]", accountId);

        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private LedgerTransferResponse mapResponse(List<LedgerEntry> entries) {
        var debit = entries.stream().filter(LedgerEntry::isDebit).findFirst().orElseThrow();
        var credit = entries.stream().filter(LedgerEntry::isCredit).findFirst().orElseThrow();

        return LedgerTransferResponse.builder()
                .transferId(debit.getTransferId())
                .creditEntry(new LedgerEntryDto(credit.getAccountId(), credit.getAmount(), credit.getType().name()))
                .debitEntry(new LedgerEntryDto(debit.getAccountId(), debit.getAmount(), debit.getType().name()))
                .createdAt(debit.getCreatedAt())
                .build();
    }

    private List<LedgerEntry> transfer(UUID transferId, BigDecimal amount, Account fromAccount, Account toAccount) {
        fromAccount.debit(amount);
        toAccount.credit(amount);

        var debitEntry = LedgerEntry.debit(transferId, fromAccount.getId(), amount);
        var creditEntry = LedgerEntry.credit(transferId, toAccount.getId(), amount);

        return List.of(debitEntry, creditEntry);

    }
}
