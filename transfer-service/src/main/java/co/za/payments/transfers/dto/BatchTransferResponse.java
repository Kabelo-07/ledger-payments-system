package co.za.payments.transfers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BatchTransferResponse(@JsonProperty("transfers") List<TransferResponse> transferResponses) { }
