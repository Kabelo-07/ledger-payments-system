package co.za.payments.ledger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record LedgerTransferResponse(
        @JsonProperty("transfer_id") UUID transferId,
        @JsonProperty("debit_entry") LedgerEntryDto debitEntry,
        @JsonProperty("credit_entry") LedgerEntryDto creditEntry,
        @JsonProperty("created_at") Instant createdAt
) { }