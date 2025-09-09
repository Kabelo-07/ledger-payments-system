package co.za.payments.ledger.service.impl;

import co.za.payments.ledger.domain.Account;
import co.za.payments.ledger.domain.EntryType;
import co.za.payments.ledger.dto.TransferRequest;
import co.za.payments.ledger.exception.InsufficientAccountBalanceException;
import co.za.payments.ledger.exception.InvalidAmountException;
import co.za.payments.ledger.repository.AccountRepository;
import co.za.payments.ledger.repository.LedgerEntryRepository;
import co.za.payments.ledger.service.LedgerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
class LedgerEntryServiceIT {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerEntryRepository ledgerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @AfterEach
    void cleanUp() {
        ledgerRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void createEntry_debitsFromAccount_creditsToAccount_createsLedgerEntries() {
        //given
        var fromAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(500)));
        var toAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(10)));
        var transferId = UUID.randomUUID();

        var request = new TransferRequest(transferId, fromAccount.getId(), toAccount.getId(), BigDecimal.valueOf(100));

        // when
        var response = ledgerService.createEntry(request);

        // then
        var updatedFromAccount = accountRepository.findById(fromAccount.getId()).orElseThrow();
        var updateToAccount = accountRepository.findById(toAccount.getId()).orElseThrow();

        assertThat(updateToAccount.getBalance()).isEqualByComparingTo("110.00");
        assertThat(updatedFromAccount.getBalance()).isEqualByComparingTo("400.00");
        var entries = ledgerRepository.findAll(Sort.by(Sort.Order.by("createdAt")));

        // and
        assertThat(entries)
                .isNotEmpty()
                .hasSize(2);
        assertThat(entries.get(0).getType()).isEqualTo(EntryType.DEBIT);
        assertThat(entries.get(0).getAccountId()).isEqualTo(fromAccount.getId());
        assertThat(entries.get(0).getAmount()).isEqualByComparingTo("100.00");
        assertThat(entries.get(1).getType()).isEqualTo(EntryType.CREDIT);
        assertThat(entries.get(1).getAccountId()).isEqualTo(toAccount.getId());
        assertThat(entries.get(1).getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void createEntry_throwsInsufficientBalanceException_whenFromAccountBalanceIsTooLow() {
        // given
        var fromAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(500)));
        var toAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(10)));
        var transferId = UUID.randomUUID();

        // and
        var transferAmount = BigDecimal.valueOf(1_000);
        var fromAccountId = fromAccount.getId();
        var toAccountId = toAccount.getId();

        // when
        var req = new TransferRequest(transferId, fromAccountId, toAccountId, transferAmount);
        assertThatExceptionOfType(InsufficientAccountBalanceException.class)
                .isThrownBy(() -> ledgerService.createEntry(req))
                .withMessage("Insufficient funds in account %s".formatted(fromAccount.getId()));
    }

    @Test
    void createEntry_throwsInvalidAmountException_whenTransferAmountIsNotValid() {
        // given
        var fromAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(500)));
        var toAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(10)));
        var transferId = UUID.randomUUID();

        // and
        var transferAmount = BigDecimal.valueOf(0);
        var fromAccountId = fromAccount.getId();
        var toAccountId = toAccount.getId();

        // when
        var req = new TransferRequest(transferId, fromAccountId, toAccountId, transferAmount);
        assertThatExceptionOfType(InvalidAmountException.class)
                .isThrownBy(() -> ledgerService.createEntry(req));
    }

    @Test
    void createEntry_returnsExistingTransfer_whenExistingTransferIdIsUsed() {
        // given from account with balance of 2500
        var fromAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(2_500)));

        // and to account with balance of 10
        var toAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(10)));

        // and
        var transferId = UUID.randomUUID();

        // when first call is made
        var firstReq = new TransferRequest(transferId, fromAccount.getId(), toAccount.getId(), BigDecimal.valueOf(2_000));
        var firstResponse = ledgerService.createEntry(firstReq);

        // and  second call is made
        var secondReq = new TransferRequest(transferId, fromAccount.getId(), toAccount.getId(), BigDecimal.valueOf(5_000));
        var secondResponse = ledgerService.createEntry(secondReq);

        // then
        assertThat(secondResponse.debitEntry().amount()).isEqualByComparingTo("2000");
        assertThat(secondResponse.debitEntry().type()).isEqualTo("DEBIT");
        assertThat(secondResponse.debitEntry().accountId()).isEqualTo(fromAccount.getId());

        assertThat(secondResponse.creditEntry().amount()).isEqualByComparingTo("2000");
        assertThat(secondResponse.creditEntry().type()).isEqualTo("CREDIT");
        assertThat(secondResponse.creditEntry().accountId()).isEqualTo(toAccount.getId());

        // and
        fromAccount = accountRepository.findById(fromAccount.getId()).orElseThrow();
        toAccount = accountRepository.findById(toAccount.getId()).orElseThrow();

        // and to account balance was increased by 2000 - once
        assertThat(toAccount.getBalance()).isEqualByComparingTo("2010.00");

        // and fro account balance was decreased by 2000 - once
        assertThat(fromAccount.getBalance()).isEqualByComparingTo("500.00");

    }
}
