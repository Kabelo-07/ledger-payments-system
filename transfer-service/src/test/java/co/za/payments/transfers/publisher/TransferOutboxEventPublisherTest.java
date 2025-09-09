package co.za.payments.transfers.publisher;

import co.za.payments.transfers.client.LedgerApiClient;
import co.za.payments.transfers.config.OutboxProperties;
import co.za.payments.transfers.domain.OutboxStatus;
import co.za.payments.transfers.domain.Transfer;
import co.za.payments.transfers.domain.TransferOutboxEvent;
import co.za.payments.transfers.domain.TransferStatus;
import co.za.payments.transfers.dto.LedgerTransferRequest;
import co.za.payments.transfers.repository.OutboxRepository;
import co.za.payments.transfers.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferOutboxEventPublisherTest {

    private TransferOutboxEventPublisher publisher;

    @Mock
    private LedgerApiClient ledgerApiClient;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private TransferRepository transferRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OutboxProperties properties;

    @BeforeEach
    void setUp() {
        var processor = new TransferOutboxEventProcessor(outboxRepository, transferRepository, ledgerApiClient,
                objectMapper, properties);
        publisher = new TransferOutboxEventPublisher(outboxRepository, processor);
    }

    @Test
    void shouldProcessPendingEvents() throws Exception {
        // given
        var transferId = UUID.randomUUID();
        var request = new LedgerTransferRequest(transferId, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100));
        var event = TransferOutboxEvent.instanceOf(transferId, objectMapper.writeValueAsString(request));

        // and
        when(outboxRepository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(event));

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(
                Transfer.instanceOf(UUID.randomUUID(), BigDecimal.valueOf(100), UUID.randomUUID()))
        );

        // when
        publisher.publishPendingEvents();

        // then
        verify(ledgerApiClient, times(1)).createLedgerEntry(any());
        verify(outboxRepository, times(1)).save(any());
        verify(transferRepository, times(1)).save(any());
    }

    @Test
    void shouldHandleRetryWhenProcessEventFailed() throws Exception {
        // given
        var transferId = UUID.randomUUID();
        var request = new LedgerTransferRequest(transferId, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100));
        var event = TransferOutboxEvent.instanceOf(transferId, objectMapper.writeValueAsString(request));

        // and
        when(properties.getBaseBackoffSeconds()).thenReturn(20);
        when(properties.getMaxRetries()).thenReturn(5);

        // and
        when(outboxRepository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(any(), any())).thenReturn(List.of(event));
        when(ledgerApiClient.createLedgerEntry(any())).thenThrow(new RuntimeException());
        when(transferRepository.findById(transferId)).thenReturn(Optional.of(
                Transfer.instanceOf(UUID.randomUUID(), BigDecimal.valueOf(100), UUID.randomUUID()))
        );

        // when
        publisher.publishPendingEvents();

        // then
        assertThat(event.getNumberOfAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // and
        verify(ledgerApiClient, times(1)).createLedgerEntry(any());
        verify(outboxRepository, times(1)).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldMarkEventAsFailed_andTransferAsFailed_whenMaxRetryReachedOnFailure() throws Exception {
        // given
        var transferId = UUID.randomUUID();
        var request = new LedgerTransferRequest(transferId, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100));

        // and
        var event = TransferOutboxEvent.instanceOf(transferId, objectMapper.writeValueAsString(request));
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // and
        when(properties.getMaxRetries()).thenReturn(1);

        // and
        when(outboxRepository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(any(), any())).thenReturn(List.of(event));
        when(ledgerApiClient.createLedgerEntry(any())).thenThrow(new RuntimeException());

        // when
        var transfer  =Transfer.instanceOf(UUID.randomUUID(), BigDecimal.valueOf(100), UUID.randomUUID());
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.PROCESSING);

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        // and
        publisher.publishPendingEvents();

        // then
        assertThat(event.getNumberOfAttempts()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.FAILED);

        // and
        verify(ledgerApiClient, times(1)).createLedgerEntry(any());
        verify(outboxRepository, times(1)).save(any());
        verify(transferRepository).save(any());
    }

}