package co.za.payments.transfers.service.impl;

import co.za.payments.transfers.config.TransferConfigProperties;
import co.za.payments.transfers.domain.Transfer;
import co.za.payments.transfers.domain.TransferOutboxEvent;
import co.za.payments.transfers.dto.*;
import co.za.payments.transfers.exception.SystemInternalException;
import co.za.payments.transfers.exception.TransferNotFoundException;
import co.za.payments.transfers.repository.IdempotencyRepository;
import co.za.payments.transfers.repository.OutboxRepository;
import co.za.payments.transfers.repository.TransferRepository;
import co.za.payments.transfers.service.TransferService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class TransferServiceImpl implements TransferService {

    private final TransferRepository repository;
    @Qualifier("transferExecutor")
    private final Executor executor;
    private final TransferConfigProperties properties;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public TransferServiceImpl(TransferRepository repository,
                               Executor executor, TransferConfigProperties properties,
                               IdempotencyRepository idempotencyRepository,
                               OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.executor = executor;
        this.properties = properties;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TransferResponse processTransfer(AccountTransferRequest request, String idempotencyKey) {
        var cachedResponse = idempotencyRepository.get(idempotencyKey, TransferResponse.class);

        if (cachedResponse.isPresent()) {
            var response = cachedResponse.get();

            log.info("IdempotencyKey: [{}], found in cache, with transferId: [{}], amount: [{}], fromAccount: [{}], toAccount: [{}]", idempotencyKey,
                    response.transferId(), response.amount(),
                    response.fromAccountId(), response.toAccountId());

            return response;
        }
        log.info("IdempotencyKey: [{}], not found in cache, processing transfer", idempotencyKey);

        var transferResponse = processSingleTransfer(request.fromAccountId(), request.amount(), request.toAccountId());

        idempotencyRepository.put(idempotencyKey, transferResponse, properties.getTtl());

        log.info("Processed transfer request: [{}]", request);
        return transferResponse;
    }

    private TransferResponse processSingleTransfer(UUID fromAccountId, BigDecimal amount, UUID toAccountId) {
        log.info("Processing transfer request fromAccountId: [{}], toAccountId: [{}], amount: [{}]", fromAccountId, toAccountId, amount);

        //create and save transfer
        var transfer =  repository.save(Transfer.instanceOf(fromAccountId, amount, toAccountId));

        //create and save outbox event
        var ledgerRequest = new LedgerTransferRequest(transfer.getId(), transfer.getFromAccountId(), transfer.getToAccountId(), transfer.getAmount());

        var transferOutbox = TransferOutboxEvent.instanceOf(transfer.getId(), convertToString(ledgerRequest));

        outboxRepository.save(transferOutbox);

        var response = mapToResponse(transfer);

        log.info("Processed transfer request fromAccountId: [{}], toAccountId: [{}], amount: [{}], generated transferId: [{}], status: [{}]",
                fromAccountId, toAccountId, amount,
                response.transferId(),
                response.status());

        return response;
    }

    @Override
    @Transactional
    public BatchTransferResponse processBatch(BatchTransferRequest batchRequest, String idempotencyKey) {
        var cached = idempotencyRepository.get(idempotencyKey, BatchTransferResponse.class);

        if (cached.isPresent()) {
            log.info("IdempotencyKey: [{}], found in cache, returning existing batch transfer", idempotencyKey);

            return cached.get();
        }

        log.info("Processing batch transfer, idempotencyKey: [{}], not found in cache", idempotencyKey);

        batchRequest.validate(properties.maxTransferSize());

        var futures = batchRequest.transferRequests()
                .stream()
                .map(req -> CompletableFuture.supplyAsync(() -> processSingleTransfer(req.fromAccountId(), req.amount(), req.toAccountId()), executor))
                .toList();

        var results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        var batchResponse = new BatchTransferResponse(results);

        idempotencyRepository.put(idempotencyKey, batchResponse, properties.getTtl());

        return batchResponse;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public TransferResponse retrieveById(UUID id) {
        log.info("Retrieving transfer with id: [{}]", id);

        return repository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new TransferNotFoundException(id));
    }

    private TransferResponse mapToResponse(Transfer transfer) {
        return new TransferResponse(transfer.getId(),
                transfer.getStatus().name(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getAmount()
        );
    }

    private String convertToString(LedgerTransferRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new SystemInternalException("Unable to process provided input, error: [%s]".formatted(e.getMessage()));
        }
    }
}
