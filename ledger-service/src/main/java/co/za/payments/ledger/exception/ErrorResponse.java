package co.za.payments.ledger.exception;

import java.util.Map;

public record ErrorResponse(int status, String code, String message, Map<String, String> errors) {

    public ErrorResponse(int status, String code, String message) {
        this(status, code, message, Map.of());
    }
}
