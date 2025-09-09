package co.za.payments.transfers.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "transfer")
public class TransferConfigProperties {

    private BatchProperties batch;
    private IdempotencyProperty idempotency;

    public int threadPoolSize() {
        return batch.threadPoolSize();
    }

    public int maxTransferSize() {
        return batch.maxTransferSize();
    }

    public int queueCapacity() {
        return batch.queueCapacity();
    }

    public Duration getTtl() {
        return idempotency.ttl();
    }

}

record BatchProperties (int threadPoolSize, int maxTransferSize, int queueCapacity) {}

record IdempotencyProperty(@NotNull Duration ttl) {}
