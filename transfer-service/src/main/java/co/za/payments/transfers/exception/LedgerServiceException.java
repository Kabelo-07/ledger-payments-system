package co.za.payments.transfers.exception;

public class LedgerServiceException extends RuntimeException {

    private final ErrorResponse response;

    public LedgerServiceException(ErrorResponse response) {
        super(response.message());
        this.response = response;
    }

    public ErrorResponse getResponse() {
        return response;
    }
}
