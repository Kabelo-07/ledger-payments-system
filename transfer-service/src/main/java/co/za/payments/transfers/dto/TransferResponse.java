package co.za.payments.transfers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        @JsonProperty("transfer_id") UUID transferId,
        String status,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("from_account_id") UUID fromAccountId,
        @JsonProperty("to_account_id") UUID toAccountId,
        BigDecimal amount) {
}
