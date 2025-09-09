package co.za.payments.ledger.service.impl;

import co.za.payments.ledger.domain.Account;
import co.za.payments.ledger.domain.LedgerEntry;
import co.za.payments.ledger.dto.TransferRequest;
import co.za.payments.ledger.exception.AccountNotFoundException;
import co.za.payments.ledger.exception.InsufficientAccountBalanceException;
import co.za.payments.ledger.exception.InvalidAmountException;
import co.za.payments.ledger.repository.AccountRepository;
import co.za.payments.ledger.repository.LedgerEntryRepository;
import co.za.payments.ledger.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerEntryServiceImplTest {

    @Mock
    private LedgerEntryRepository ledgerRepository;
    @Mock
    private AccountRepository accountRepository;

    private LedgerService ledgerService;

    private UUID fromAccountId;
    private UUID toAccountId;
    private UUID transferId;

    @BeforeEach
    void setUp() {
        fromAccountId = UUID.randomUUID();
        toAccountId = UUID.randomUUID();
        transferId = UUID.randomUUID();
        ledgerService = new LedgerEntryServiceImpl(ledgerRepository, accountRepository);
    }

    @Test
    void createEntry_createsLedgerEntriesAndReturnsResponse() {
        // given
        var fromAccount = Account.instanceOf(BigDecimal.valueOf(500));
        var toAccount = Account.instanceOf(BigDecimal.valueOf(450));

        // when
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        // and
        var debit = LedgerEntry.debit(transferId, fromAccountId, BigDecimal.valueOf(100));
        var credit = LedgerEntry.credit(transferId, toAccountId, BigDecimal.valueOf(100));

        when(ledgerRepository.saveAll(any())).thenReturn(List.of(debit, credit));

        var response = ledgerService.createEntry(new TransferRequest(transferId, fromAccountId, toAccountId, BigDecimal.valueOf(100)));

        // then
        assertThat(response.debitEntry().amount()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(response.debitEntry().type()).isEqualTo("DEBIT");
        assertThat(response.debitEntry().accountId()).isEqualTo(fromAccountId);

        assertThat(response.creditEntry().amount()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(response.creditEntry().type()).isEqualTo("CREDIT");
        assertThat(response.creditEntry().accountId()).isEqualTo(toAccountId);

        verify(ledgerRepository).findByTransferId(any());
        verify(ledgerRepository, times(1)).saveAll(any());
    }

    @Test
    void createEntry_returnsExistingLedgerEntryResponse() {
        // given
        var debit = LedgerEntry.debit(transferId, fromAccountId, BigDecimal.valueOf(21_349));
        var credit = LedgerEntry.credit(transferId, fromAccountId, BigDecimal.valueOf(21_349));

        // when
        when(ledgerRepository.findByTransferId(any())).thenReturn(List.of(debit, credit));

        var response = ledgerService.createEntry(new TransferRequest(transferId, fromAccountId, toAccountId, BigDecimal.valueOf(100)));

        // then
        assertThat(response.debitEntry().amount()).isEqualTo(BigDecimal.valueOf(21_349));
        assertThat(response.debitEntry().type()).isEqualTo("DEBIT");
        assertThat(response.debitEntry().accountId()).isEqualTo(fromAccountId);

        assertThat(response.creditEntry().amount()).isEqualTo(BigDecimal.valueOf(21_349));
        assertThat(response.creditEntry().type()).isEqualTo("CREDIT");
        assertThat(response.creditEntry().accountId()).isEqualTo(fromAccountId);

        verify(ledgerRepository).findByTransferId(transferId);
        verify(ledgerRepository, never()).saveAll(any());
    }

    @Test
    void createEntry_throwsInsufficientBalanceException_whenAccountBalanceIsLess() {
        // given
        var fromAccount = Account.instanceOf(BigDecimal.valueOf(10));
        var toAccount = Account.instanceOf(BigDecimal.valueOf(400));

        // when
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        // then
        var amount = BigDecimal.valueOf(500);
        assertThatThrownBy( () -> ledgerService.createEntry(new TransferRequest(transferId, fromAccountId, toAccountId, amount)))
                .isInstanceOf(InsufficientAccountBalanceException.class)
                .hasMessageContaining("Insufficient funds in account");


        verify(ledgerRepository, never()).saveAll(any());
    }

    @Test
    void createEntry_throwsInvalidAmountException_whenDebitTransferAmountIsNotValid() {
        // given
        var fromAccount = Account.instanceOf(BigDecimal.valueOf(10));
        var toAccount = Account.instanceOf(BigDecimal.valueOf(400));

        // when
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        // then
        assertThatThrownBy( () -> ledgerService.createEntry(new TransferRequest(transferId, fromAccountId, toAccountId, BigDecimal.ZERO)))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void createEntry_throwsAccountNotFoundException_whenFromAccountDoesNotExist() {
        // when
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.empty());

        // then
        var amount = BigDecimal.valueOf(10_00);

        assertThatExceptionOfType(AccountNotFoundException.class)
                .isThrownBy( () -> ledgerService.createEntry(new TransferRequest(transferId, fromAccountId, toAccountId, amount)))
                .withMessage("Account with ID: %s does not exist".formatted(fromAccountId));
    }

    @Test
    void createEntry_throwsAccountNotFoundException_whenToAccountDoesNotExist() {
        // when
        var fromAccount = Account.instanceOf(BigDecimal.valueOf(10));

        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.empty());

        // then
        var amount = BigDecimal.valueOf(10_00);

        assertThatExceptionOfType(AccountNotFoundException.class)
                .isThrownBy( () -> ledgerService.createEntry(new TransferRequest(transferId, fromAccountId, toAccountId, amount)))
                .withMessage("Account with ID: %s does not exist".formatted(toAccountId));
    }

}