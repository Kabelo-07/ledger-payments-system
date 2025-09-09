package co.za.payments.ledger.api.controller;


import co.za.payments.ledger.service.LedgerService;
import co.za.payments.ledger.dto.LedgerTransferResponse;
import co.za.payments.ledger.dto.TransferRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
public class LedgerEntryController {

    private final LedgerService ledgerService;

    @PostMapping("/transfer")
    public ResponseEntity<LedgerTransferResponse> recordTransfer(@RequestBody @Valid TransferRequest request) {
        var response = ledgerService.createEntry(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
