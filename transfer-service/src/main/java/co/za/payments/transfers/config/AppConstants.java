package co.za.payments.transfers.config;

public final class AppConstants {

    private AppConstants() { }

    public static final String REQUEST_ID_HEADER_NAME = "X-Request-ID";
    public static final String INVALID_BATCH_SIZE = "INVALID_BATCH_SIZE";
    public static final String TRANSFER_NOT_FOUND = "TRANSFER_NOT_FOUND";

    public static final String INVALID_REQUEST = "INVALID_REQUEST";

    public static final String MISSING_HEADER = "MISSING_HEADER";
    public static final String CONFLICT_CODE = "TRANSFER_CONFLICT";
}
