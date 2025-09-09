package co.za.payments.transfers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountTransferRequest(
        @NotNull
        @JsonProperty("from_account_id") UUID fromAccountId,
        @NotNull
        @JsonProperty("to_account_id") UUID toAccountId,
        @NotNull
        @Min(1) BigDecimal amount) {
}
