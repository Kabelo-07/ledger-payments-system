package co.za.payments.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotNull(message = "balance amount must not be null")
        @DecimalMin(value = "1.0", message = "Minimum balance of 1.0 is required")
        BigDecimal balance
) { }
