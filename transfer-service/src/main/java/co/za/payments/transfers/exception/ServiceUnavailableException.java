package co.za.payments.transfers.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
