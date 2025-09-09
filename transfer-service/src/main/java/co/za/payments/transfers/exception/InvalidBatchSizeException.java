package co.za.payments.transfers.exception;

import static co.za.payments.transfers.config.AppConstants.INVALID_BATCH_SIZE;

public class InvalidBatchSizeException extends TransferApplicationException {
    public InvalidBatchSizeException(int size, int maxSize) {
        super(INVALID_BATCH_SIZE, "Batch size of %d exceeds max allowed size of %d".formatted(
                size, maxSize));
    }

    public InvalidBatchSizeException(String message) {
        super(INVALID_BATCH_SIZE, message);
    }
}
