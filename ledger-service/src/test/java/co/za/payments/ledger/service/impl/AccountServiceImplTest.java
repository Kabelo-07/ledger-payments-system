package co.za.payments.ledger.service.impl;

import co.za.payments.ledger.domain.Account;
import co.za.payments.ledger.dto.CreateAccountRequest;
import co.za.payments.ledger.exception.AccountNotFoundException;
import co.za.payments.ledger.repository.AccountRepository;
import co.za.payments.ledger.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository repository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(repository);
    }

    @Test
    void create_createsAccountWithInitialBalance_andReturnsAResponse() {
        // given
        var initialBalance  = BigDecimal.valueOf(500_123);

        var account = Account.instanceOf(initialBalance);

        // when
        when(repository.save(any())).thenReturn(account);

        var response = accountService.create(new CreateAccountRequest(initialBalance));

        // then
        assertThat(response).isNotNull();
        assertThat(response.balance()).isEqualTo(initialBalance);
    }

    @Test
    void getAccount_throwsAccountNotFoundException_whenAccountDoesNotExist() {
        // given
        var accountId = UUID.randomUUID();

        // when
        when(repository.findById(accountId)).thenReturn(Optional.empty());

        // then
        assertThatExceptionOfType(AccountNotFoundException.class)
                .isThrownBy(() -> accountService.getAccount(accountId))
                .withMessage("Account with ID: %s does not exist".formatted(accountId));
    }

    @Test
    void getAccount_returnsResponse_whenAccountExist() {
        // given
        var accountId = UUID.randomUUID();
        var account = Account.instanceOf(BigDecimal.valueOf(500_123));

        when(repository.findById(accountId)).thenReturn(Optional.of(account));

        var response = accountService.getAccount(accountId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.balance()).isEqualTo(account.getBalance());
    }
}