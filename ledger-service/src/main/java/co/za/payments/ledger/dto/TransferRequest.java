package co.za.payments.ledger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "transferId must not be null")
        @JsonProperty("transfer_id") UUID transferId,
        @NotNull(message = "fromAccount must not be null")
        @JsonProperty("from_account_id") UUID fromAccountId,
        @NotNull(message = "toAccount must not be null")
        @JsonProperty("to_account_id")UUID toAccountId,
        @NotNull(message = "transfer amount must not be null")
        @DecimalMin(value = "1.0", message = "Minimum transfer amount of 1.0 is required")
        BigDecimal amount
) { }
