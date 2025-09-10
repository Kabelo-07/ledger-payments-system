package co.za.payments.transfers.publisher;

import co.za.payments.transfers.client.LedgerApiClient;
import co.za.payments.transfers.config.OutboxProperties;
import co.za.payments.transfers.domain.Transfer;
import co.za.payments.transfers.domain.TransferOutboxEvent;
import co.za.payments.transfers.dto.LedgerTransferRequest;
import co.za.payments.transfers.repository.OutboxRepository;
import co.za.payments.transfers.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferOutboxEventProcessor {

    private final OutboxRepository outboxRepository;
    private final TransferRepository transferRepository;
    private final LedgerApiClient ledgerApiClient;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;

    @Transactional
    public void processEventTransactional(TransferOutboxEvent event) {
        event.incrementRetryCount();
        var transfer = transferRepository.findById(event.getTransferId()).orElseThrow();

        log.info("Processing transfer {}, attempt {}", transfer.getId(), event.getNumberOfAttempts());

        try {
            var request = objectMapper.readValue(event.getPayload(), LedgerTransferRequest.class);
            ledgerApiClient.createLedgerEntry(request);

            this.markAsCompleted(event, transfer);
        } catch (Exception exception) {
            handleRetry(event, transfer, exception);
        }
    }

    private void handleRetry(TransferOutboxEvent event, Transfer transfer, Throwable throwable) {
        if (!event.canRetry(properties.getMaxRetries())) {
            this.markAsFailed(event, transfer);
            return;
        }

        event.setNextAttemptAt(backOffPolicyAttempt(event.getNumberOfAttempts()));
        outboxRepository.save(event);

        log.warn("Transfer {} retry scheduled to be processed at: {}, due to error: {}",
                transfer.getId(),
                event.getNextAttemptAt(),
                throwable.getMessage()
        );
    }

    private Instant backOffPolicyAttempt(int attempts) {
        long backoffSeconds = (long) (properties.getBaseBackoffSeconds() * Math.pow(2, attempts - 1));
        return Instant.now().plusSeconds(backoffSeconds);
    }


    private void markAsFailed(TransferOutboxEvent event, Transfer transfer) {
        //mark transaction as failed
        transfer.markAsFailed();
        transferRepository.save(transfer);

        //mark event as failed
        event.markAsFailed();
        outboxRepository.save(event);
    }

    private void markAsCompleted(TransferOutboxEvent event, Transfer transfer) {
        //mark event as sent
        event.markAsProcessed();
        outboxRepository.save(event);

        //mark transfer as completed
        transfer.markAsCompleted();
        transferRepository.save(transfer);
    }

}