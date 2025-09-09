package co.za.payments.transfers.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "ledger.service")
public class LedgerServiceProperties {

    @NotBlank(message = "Ledger Service baseUrl is required")
    @Pattern(
            regexp = "https?://.*",
            message = "baseUrl must start with http:// or https://"
    )
    private String baseUrl;
}
