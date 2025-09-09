package co.za.payments.transfers.api;

import co.za.payments.transfers.dto.AccountTransferRequest;
import co.za.payments.transfers.dto.BatchTransferRequest;
import co.za.payments.transfers.dto.BatchTransferResponse;
import co.za.payments.transfers.dto.TransferResponse;
import co.za.payments.transfers.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> initiateTransfer(@RequestBody @Valid AccountTransferRequest request,
                                                             @RequestHeader(name = "Idempotency-Key") String idempotencyKey) {
        var transferResponse = transferService.processTransfer(request, idempotencyKey);
        return ResponseEntity.created(URI.create("/transfers/%s".formatted(transferResponse.transferId())))
                .body(transferResponse);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchTransferResponse> initiateTransfers(@RequestBody @Valid BatchTransferRequest batchTransferRequest,
                                                                   @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        var responses =  transferService.processBatch(batchTransferRequest, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> retrieveTransfer(@PathVariable UUID id) {
        var response = transferService.retrieveById(id);
        return ResponseEntity.ok(response);
    }

}
