package co.za.payments.transfers.service;

import co.za.payments.transfers.dto.AccountTransferRequest;
import co.za.payments.transfers.dto.BatchTransferRequest;
import co.za.payments.transfers.dto.BatchTransferResponse;
import co.za.payments.transfers.dto.TransferResponse;

import java.util.UUID;

public interface TransferService {

    TransferResponse processTransfer(AccountTransferRequest request, String idempotencyHeaderKey);

    BatchTransferResponse processBatch(BatchTransferRequest batchRequest, String idempotencyHeaderKey);

    TransferResponse retrieveById(UUID id);
}
