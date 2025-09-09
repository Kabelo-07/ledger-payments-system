package co.za.payments.ledger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record AccountResponse(UUID id,
                              @JsonProperty("account_number") String accountNumber,
                              BigDecimal balance,
                              @JsonProperty("created_at") Instant createdAt) { }
