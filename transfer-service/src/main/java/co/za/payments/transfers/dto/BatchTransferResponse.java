package co.za.payments.transfers.dto;

import java.util.List;

public record BatchTransferResponse(List<TransferResponse> transferResponses) { }
