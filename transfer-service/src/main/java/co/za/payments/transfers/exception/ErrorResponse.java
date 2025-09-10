package co.za.payments.transfers.exception;

import java.util.Map;

public record ErrorResponse(Integer status, String code, String message, Map<String, String> errors) {

    public ErrorResponse(int status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public ErrorResponse(String code, String message) {
        this(null, code, message, Map.of());
    }

    public ErrorResponse(String code, String message, Map<String, String> errors) {
        this(null, code, message, errors);
    }
}
