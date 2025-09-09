package co.za.payments.ledger.api.controller;

import co.za.payments.ledger.domain.Account;
import co.za.payments.ledger.domain.LedgerEntry;
import co.za.payments.ledger.dto.TransferRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static co.za.payments.ledger.config.AppConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerEntryControllerIT extends AbstractMvcIT {

    @Test
    void shouldReturnHttp201Response_whenLedgerEntriesCreated() throws Exception {
        var transferId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(300);
        var fromAccountId = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(5_000))).getId();
        var toAccountId = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(250))).getId();

        var request = new TransferRequest(transferId, fromAccountId, toAccountId, amount);

        var resultActions = mockMvc.perform(post("/ledger/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.transfer_id", equalTo(String.valueOf(transferId))))
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andExpect(jsonPath("$.debit_entry.account_id", equalTo(String.valueOf(fromAccountId))))
                .andExpect(jsonPath("$.debit_entry.amount", equalTo(300)))
                .andExpect(jsonPath("$.debit_entry.type", equalTo("DEBIT")))
                .andExpect(jsonPath("$.credit_entry.account_id", equalTo(String.valueOf(toAccountId))))
                .andExpect(jsonPath("$.credit_entry.amount", equalTo(300)))
                .andExpect(jsonPath("$.credit_entry.type", equalTo("CREDIT")));

    }

    @Test
    void shouldReturnHttp422Response_whenAccountHasInsufficientFunds() throws Exception {
        var fromAccountId = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(20))).getId();
        var toAccountId = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(30))).getId();

        var transferId = UUID.randomUUID();
        var transferAmount = BigDecimal.valueOf(300);

        var request = new TransferRequest(transferId, fromAccountId, toAccountId, transferAmount);

        var resultActions = mockMvc.perform(post("/ledger/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", equalTo(INSUFFICIENT_BAL)))
                .andExpect(jsonPath("$.message", startsWith("Insufficient funds in account")));
    }

    @Test
    void shouldReturnHttp400Response_whenTransferAmountIsLessThanMinimum() throws Exception {
        var fromAccountId = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(20))).getId();
        var toAccountId = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(30))).getId();

        var transferId = UUID.randomUUID();
        var transferAmount = BigDecimal.valueOf(0);

        var request = new TransferRequest(transferId, fromAccountId, toAccountId, transferAmount);

        var resultActions = mockMvc.perform(post("/ledger/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(INVALID_REQUEST)))
                .andExpect(jsonPath("$.message", equalTo("Validation failed")))
                .andExpect(jsonPath("$.errors", aMapWithSize(1)))
                .andExpect(jsonPath("$.errors.amount", equalTo("Minimum transfer amount of 1.0 is required")));
    }

    @Test
    void shouldReturnHttp404Response_whenDebitAccountDoesNotExist() throws Exception {
        var toAccountId = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(30))).getId();

        var transferId = UUID.randomUUID();
        var transferAmount = BigDecimal.valueOf(50);

        var request = new TransferRequest(transferId, UUID.randomUUID(), toAccountId, transferAmount);

        var resultActions = mockMvc.perform(post("/ledger/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(ACCOUNT_NOT_FOUND_CODE)))
                .andExpect(jsonPath("$.message", startsWith("Account with ID")));
    }

    @Test
    void shouldReturnHttp404Response_whenCreditAccountDoesNotExist() throws Exception {
        var fromAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(30))).getId();

        var transferId = UUID.randomUUID();
        var transferAmount = BigDecimal.valueOf(50);

        var request = new TransferRequest(transferId, fromAccount, UUID.randomUUID(), transferAmount);

        var resultActions = mockMvc.perform(post("/ledger/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(ACCOUNT_NOT_FOUND_CODE)))
                .andExpect(jsonPath("$.message", startsWith("Account with ID")));
    }

    /**
     * <p>Testing idempotency</p>
     * @throws Exception
     */
    @Test
    void shouldReturnHttp201_withExistingTransferResponse_whenExistingTransferIdIsUsed() throws Exception {
        // given existing from and to accounts
        var fromAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(100)));
        var toAccount = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(20)));

        // and
        var transferId = UUID.randomUUID();

        // and existing ledger entries for same transferId with amount 20_000
        ledgerRepository.saveAll(List.of(
                LedgerEntry.debit(transferId, fromAccount.getId(), BigDecimal.valueOf(20_000)),
                LedgerEntry.credit(transferId, toAccount.getId(), BigDecimal.valueOf(20_000)))
        );

        // when initiating transfer using same transferId with amount 88

        var transferAmount = BigDecimal.valueOf(88);
        var request = new TransferRequest(transferId, fromAccount.getId(), toAccount.getId(), transferAmount);
        var resultActions = mockMvc.perform(post("/ledger/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)));

        // then transfer of 20_000 must be returned
        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.transfer_id", equalTo(String.valueOf(transferId))))
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andExpect(jsonPath("$.debit_entry.account_id", equalTo(String.valueOf(fromAccount.getId()))))
                .andExpect(jsonPath("$.debit_entry.amount", equalTo(20000.00)))
                .andExpect(jsonPath("$.debit_entry.type", equalTo("DEBIT")))
                .andExpect(jsonPath("$.credit_entry.account_id", equalTo(String.valueOf(toAccount.getId()))))
                .andExpect(jsonPath("$.credit_entry.amount", equalTo(20000.00)))
                .andExpect(jsonPath("$.credit_entry.type", equalTo("CREDIT")));

        // and no entry of amount 88 exists
        assertThat(ledgerRepository
                .findAll()
                .stream()
                .filter(entry -> entry.getAmount().compareTo(BigDecimal.valueOf(88)) == 0)
        ).isEmpty();

        // and for verification - the 20_000 entry exists
        assertThat(ledgerRepository
                .findAll()
                .stream()
                .filter(entry -> entry.getAmount().compareTo(BigDecimal.valueOf(20_000)) == 0)
        ).isNotEmpty();
    }

}