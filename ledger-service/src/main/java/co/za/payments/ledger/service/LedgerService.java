package co.za.payments.ledger.service;

import co.za.payments.ledger.dto.LedgerTransferResponse;
import co.za.payments.ledger.dto.TransferRequest;

public interface LedgerService {

    LedgerTransferResponse createEntry(TransferRequest request);

}
