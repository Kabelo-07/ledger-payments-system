package co.za.payments.transfers.client.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record LedgerEntryDto (
        @JsonProperty("account_id") UUID accountId,
        BigDecimal amount,
        @JsonProperty("type") String type) {}
