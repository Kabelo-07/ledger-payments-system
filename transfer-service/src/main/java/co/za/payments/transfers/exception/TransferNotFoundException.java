package co.za.payments.transfers.exception;

import java.util.UUID;

import static co.za.payments.transfers.config.AppConstants.TRANSFER_NOT_FOUND;

public class TransferNotFoundException extends TransferApplicationException {

    public TransferNotFoundException(UUID transferId) {
        super(TRANSFER_NOT_FOUND, "Transfer with ID: %s not found".formatted(transferId));
    }
}
