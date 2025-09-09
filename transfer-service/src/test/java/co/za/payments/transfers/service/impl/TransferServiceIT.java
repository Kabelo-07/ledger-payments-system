package co.za.payments.transfers.service.impl;

import co.za.payments.transfers.domain.Transfer;
import co.za.payments.transfers.dto.AccountTransferRequest;
import co.za.payments.transfers.dto.TransferResponse;
import co.za.payments.transfers.exception.TransferNotFoundException;
import co.za.payments.transfers.repository.IdempotencyRepository;
import co.za.payments.transfers.repository.OutboxRepository;
import co.za.payments.transfers.repository.TransferRepository;
import co.za.payments.transfers.service.TransferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Testcontainers
class TransferServiceIT {

    @Container
    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(6379);

    @Autowired
    private TransferService transferService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
        transferRepository.deleteAll();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);
    }

    @Test
    void shouldCreateTransferAndWriteToOutbox() {
        // given
        var fromAccount = UUID.randomUUID();
        var toAccount = UUID.randomUUID();
        var amount = BigDecimal.valueOf(200);
        var idempotencyKey = UUID.randomUUID().toString();

        var request = new AccountTransferRequest(fromAccount, toAccount, amount);

        // when
        TransferResponse response = transferService.processTransfer(request, idempotencyKey);

        // Then: outbox entry created
        var events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getPayload()).contains(fromAccount.toString(), toAccount.toString());

        // And: idempotency stored
        var cachedResponse = idempotencyRepository.get(idempotencyKey, TransferResponse.class);

        assertThat(cachedResponse)
                .isPresent()
                .contains(response);
    }

    @Test
    void shouldReturnSameResponseWhenDuplicateIdempotencyKeyUsed() {
        // given
        var fromAccount = UUID.randomUUID();
        var toAccount = UUID.randomUUID();
        var amount = BigDecimal.valueOf(200);
        var idempotencyKey = String.valueOf(System.currentTimeMillis());

        // first call, same idempotent key
        var request = new AccountTransferRequest(fromAccount, toAccount, amount);
        TransferResponse firstResponse = transferService.processTransfer(request, idempotencyKey);

        // second call, same idempotent key
        var newRequest = new AccountTransferRequest(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(444));
        TransferResponse secondResponse = transferService.processTransfer(newRequest, idempotencyKey);

        // then responses identical
        assertThat(secondResponse).isEqualTo(firstResponse);

        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldReturnResponseWhenGivenTransferIdIsValid() {
        // given
        var fromAccount = UUID.randomUUID();
        var toAccount = UUID.randomUUID();
        var amount = BigDecimal.valueOf(200);

        // and transfer exists
        var transfer = transferRepository.save(Transfer.instanceOf(fromAccount, amount, toAccount));

        var savedTransfer = transferService.retrieveById(transfer.getId());

        // then transfer is located
        assertThat(savedTransfer).isNotNull();
    }

    @Test
    void shouldThrowAccountNoFoundException_whenTransferIdIsInvalid() {
        // given
        var transferId = UUID.randomUUID();

        // then
        assertThatExceptionOfType(TransferNotFoundException.class)
                .isThrownBy(() -> transferService.retrieveById(transferId))
                .withMessageStartingWith("Transfer with ID");

    }
}