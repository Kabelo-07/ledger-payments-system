package co.za.payments.transfers.service.impl;

import co.za.payments.transfers.config.TransferConfigProperties;
import co.za.payments.transfers.domain.Transfer;
import co.za.payments.transfers.domain.TransferStatus;
import co.za.payments.transfers.dto.AccountTransferRequest;
import co.za.payments.transfers.dto.BatchTransferRequest;
import co.za.payments.transfers.dto.BatchTransferResponse;
import co.za.payments.transfers.dto.TransferResponse;
import co.za.payments.transfers.exception.InvalidBatchSizeException;
import co.za.payments.transfers.exception.TransferNotFoundException;
import co.za.payments.transfers.repository.IdempotencyRepository;
import co.za.payments.transfers.repository.OutboxRepository;
import co.za.payments.transfers.repository.TransferRepository;
import co.za.payments.transfers.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private TransferRepository transferRepository;

    private final Executor executor = Runnable::run;
    @Mock
    private TransferConfigProperties properties;
    @Mock
    private IdempotencyRepository idempotencyRepository;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;
    
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferServiceImpl(transferRepository, executor, properties, idempotencyRepository,
                outboxRepository, objectMapper);
    }

    @Test
    void processTransfer_createsTransferAndOutboxEventWithIdempotency() {
        // given
        var transfer = savedTransfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(189));
        var transferRequest = newAccountTransfer(transfer.getFromAccountId(), transfer.getToAccountId(), transfer.getAmount());

        // and
        when(idempotencyRepository.get(any(), any())).thenReturn(Optional.empty());
        when(transferRepository.save(any())).thenReturn(transfer);

        // when
        var response = transferService.processTransfer(transferRequest, UUID.randomUUID().toString());

        // then
        assertThat(response).isNotNull();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.fromAccountId()).isEqualTo(transfer.getFromAccountId());
        assertThat(response.toAccountId()).isEqualTo(transfer.getToAccountId());
        assertThat(response.amount()).isEqualTo(transfer.getAmount());

        // and
        verify(idempotencyRepository).put(any(), any(), any());
        verify(outboxRepository).save(any());
    }

    @Test
    void processTransfer_returnsCachedTransferWithIdempotency() {
        // given
        var transferId = UUID.randomUUID();
        var fromAccountId  =UUID.randomUUID();
        var toAccountId  =UUID.randomUUID();

        // with amount of 500
        var amount = BigDecimal.valueOf(500);

        // and
        var idempotencyKey = UUID.randomUUID().toString();
        var existingTransfer = transferResponse(transferId, amount, fromAccountId, toAccountId);

        when(idempotencyRepository.get(idempotencyKey, TransferResponse.class)).thenReturn(Optional.of(existingTransfer));

        // and transfer request with amount of 10_000
        var transferRequest = newAccountTransfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(10_000));

        // when
        var response = transferService.processTransfer(transferRequest, idempotencyKey);

        // then
        assertThat(response).isNotNull();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.fromAccountId()).isEqualTo(fromAccountId);
        assertThat(response.toAccountId()).isEqualTo(toAccountId);

        //and amount in response is not 10_000
        assertThat(response.amount()).isNotEqualTo(transferRequest.amount());

        // and
        verify(idempotencyRepository, never()).put(any(), any(), any());
        verify(outboxRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    void processBatch_createsTransfersAndOutboxEvents() {
        // given
        var transfer = savedTransfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(189));

        var transferRequests = newBatchTransfers(4);

        // and
        when(idempotencyRepository.get(any(), any())).thenReturn(Optional.empty());
        when(transferRepository.save(any())).thenReturn(transfer);
        when(properties.maxTransferSize()).thenReturn(10);

        // when
        var response = transferService.processBatch(
                new BatchTransferRequest(transferRequests),
                UUID.randomUUID().toString()
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.transferResponses()).isNotEmpty();
        assertThat(response.transferResponses()).hasSize(4);

        // and
        verify(idempotencyRepository).put(any(), any(), any());
        verify(outboxRepository, times(4)).save(any());
    }

    @Test
    void processBatch_returnsCachedResponseWithIdempotency() {
        // given
        var transfer = savedTransfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(189));
        var transferId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(450);
        var fromAccountId = UUID.randomUUID();
        var toAccountId = UUID.randomUUID();

        // and
        var idempotencyKey = UUID.randomUUID().toString();

        //and existing cached batchTransfer has 5 transfer transactions
        var existingTransfers = generate(5);
        var existingBatchTransfer =  new BatchTransferResponse(existingTransfers);

        // and
        when(idempotencyRepository.get(any(), any())).thenReturn(Optional.of(existingBatchTransfer));

        // when
        var transferRequests = newBatchTransfers(2);
        var batchRequest = new BatchTransferRequest(transferRequests);
        var response = transferService.processBatch(batchRequest, idempotencyKey);

        // then
        assertThat(response).isNotNull();
        assertThat(response.transferResponses()).isNotEmpty();

        // and verify response returned has 5 transfer transactions
        assertThat(response.transferResponses()).hasSize(5);

        // and
        verify(idempotencyRepository, never()).put(any(), any(), any());
        verify(transferRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processBatch_throwsInvalidBatchSizeException_whenBatchSizeExceedsMax() {
        // given
        var transfer = savedTransfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(189));
        var transfers = newBatchTransfers(10);

        // and
        var batchRequest = new BatchTransferRequest(transfers);
        var idempotencyKey =  UUID.randomUUID().toString();

        // when
        when(idempotencyRepository.get(any(), any())).thenReturn(Optional.empty());
        when(properties.maxTransferSize()).thenReturn(6);

        // then
        assertThatExceptionOfType(InvalidBatchSizeException.class)
                .isThrownBy(() -> transferService.processBatch(batchRequest, idempotencyKey))
                .withMessageStartingWith("Batch size of");
    }

    @Test
    void processBatch_throwsInvalidBatchSizeException_whenProvidedBatchIsEmpty() {
        // given
        var idempotencyKey =  UUID.randomUUID().toString();

        // when
        when(idempotencyRepository.get(any(), any())).thenReturn(Optional.empty());
        when(properties.maxTransferSize()).thenReturn(6);

        // then
        assertThatExceptionOfType(InvalidBatchSizeException.class)
                .isThrownBy(() -> transferService.processBatch(new BatchTransferRequest(List.of()), idempotencyKey))
                .withMessageStartingWith("At-least");
    }

    @Test
    void processBatch_throwsInvalidBatchSizeException_whenProvidedBatchIsNull() {
        // given
        var idempotencyKey =  UUID.randomUUID().toString();

        // when
        when(idempotencyRepository.get(any(), any())).thenReturn(Optional.empty());
        when(properties.maxTransferSize()).thenReturn(6);

        // then
        assertThatExceptionOfType(InvalidBatchSizeException.class)
                .isThrownBy(() -> transferService.processBatch(new BatchTransferRequest(null), idempotencyKey))
                .withMessageStartingWith("At-least");
    }

    @Test
    void retrieveById_returnsTransferResponseWhenValidIdIsProvided() {
        // given
        var transferId = UUID.randomUUID();
        var transfer = savedTransfer(transferId, UUID.randomUUID(), BigDecimal.valueOf(189));
        when(transferRepository.findById(any())).thenReturn(Optional.of(transfer));

        // when
        var response = transferService.retrieveById(transferId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.fromAccountId()).isEqualTo(transfer.getFromAccountId());
        assertThat(response.toAccountId()).isEqualTo(transfer.getToAccountId());
        assertThat(response.amount()).isEqualTo(transfer.getAmount());
    }

    @Test
    void retrieveById_throwsTransferNotFoundException_whenInvalidIdIsProvided() {
        // given
        var transferId = UUID.randomUUID();
        when(transferRepository.findById(any())).thenReturn(Optional.empty());

        // then
        assertThatExceptionOfType(TransferNotFoundException.class)
                .isThrownBy(() -> transferService.retrieveById(transferId))
                .withMessageStartingWith("Transfer with ID");

    }


    private Transfer savedTransfer(UUID fromAccountId, UUID toAccountId, BigDecimal transferAmount) {
        var transfer = Transfer.instanceOf(fromAccountId, transferAmount, toAccountId);
        transfer.setId(UUID.randomUUID());
        return transfer;
    }

    private TransferResponse transferResponse(UUID transferId, BigDecimal amount, UUID fromAccountId, UUID toAccountId) {
        return new TransferResponse(transferId,
                TransferStatus.COMPLETED.name(),
                Instant.now(),
                Instant.now(),
                fromAccountId,
                toAccountId,
                amount);
    }

    private List<TransferResponse> generate(int size) {
        var responses = new ArrayList<TransferResponse>();
        for (var x=0; x<size; x++) {
            responses.add(transferResponse(UUID.randomUUID(), BigDecimal.valueOf(450), UUID.randomUUID(), UUID.randomUUID()));
        }

        return responses;
    }
    private List<AccountTransferRequest> newBatchTransfers(int size) {
        var requests = new ArrayList<AccountTransferRequest>();
        for (int x=0; x<size; x++) {
            requests.add(newAccountTransfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(500)));
        }
        return requests;
    }

    private AccountTransferRequest newAccountTransfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount) {
        return new AccountTransferRequest(fromAccountId, toAccountId, amount);
    }


}