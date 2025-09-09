package co.za.payments.transfers.exception;

public class LedgerServiceException extends TransferApplicationException {

    private final int status;

    public LedgerServiceException(int status, String code, String message) {
        super(code, message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
