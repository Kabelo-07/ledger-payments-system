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
        log.info("Job [START] - Retrieving scheduled transfers");
        var outboxEvents = outboxRepository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                Instant.now()
        );

        log.info("Job [START] - Found [{}] events to be processed", outboxEvents.size());

        outboxEvents.forEach(this::processEvent);

        log.info("Job [FINISH] - Sent [{}] events to be processed", outboxEvents.size());
    }

    @Async("transferExecutor")
    public void processEvent(TransferOutboxEvent event) {
        processor.processEventTransactional(event);
    }

}