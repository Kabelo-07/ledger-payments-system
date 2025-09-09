package co.za.payments.transfers.exception;

public class TransferApplicationException extends RuntimeException {
    private final String code;

    public TransferApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
