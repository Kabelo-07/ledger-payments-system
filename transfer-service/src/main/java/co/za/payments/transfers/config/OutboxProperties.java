package co.za.payments.transfers.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Data
@ConfigurationProperties(prefix = "transfer.outbox")
@Validated
public class OutboxProperties {

    @Min(value = 5, message = "max-retries must have a minimum value of 5")
    private int maxRetries;

    @Min(value = 5, message = "base-backoff-seconds must have a minimum value of 5")
    private int baseBackoffSeconds;

}

