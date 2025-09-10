package co.za.payments.transfers.exception;

public class SystemInternalException extends TransferApplicationException {

    public SystemInternalException(String message) {
        super("INTERNAL_ERROR", message);
    }

    public SystemInternalException(String code, String message) {
        super(code, message);
    }

}
