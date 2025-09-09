package co.za.payments.transfers.publisher;

import co.za.payments.transfers.domain.OutboxStatus;
import co.za.payments.transfers.domain.TransferOutboxEvent;
import co.za.payments.transfers.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferOutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final TransferOutboxEventProcessor processor;

    @Scheduled(fixedDelay = 30000)
    public void publishPendingEvents() {
        var outboxEvents = outboxRepository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                Instant.now()
        );
        outboxEvents.forEach(this::processEvent);
    }

    @Async("transferExecutor")
    public void processEvent(TransferOutboxEvent event) {
        processor.processEventTransactional(event);
    }

}