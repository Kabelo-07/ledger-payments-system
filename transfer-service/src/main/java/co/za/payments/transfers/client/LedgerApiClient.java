package co.za.payments.transfers.client;

import co.za.payments.transfers.client.contract.LedgerTransferResponse;
import co.za.payments.transfers.dto.LedgerTransferRequest;
import co.za.payments.transfers.exception.ServiceUnavailableException;
import co.za.payments.transfers.exception.TransferApplicationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ledger-service", url = "${ledger.service.base-url}")
public interface LedgerApiClient {

    Logger log = LoggerFactory.getLogger(LedgerApiClient.class);

    @PostMapping(value = "/ledger/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    @CircuitBreaker(name = "ledgerApiCircuitBreaker", fallbackMethod = "createLedgerEntryFallback")
    LedgerTransferResponse createLedgerEntry(@RequestBody @Valid LedgerTransferRequest request);

    default LedgerTransferResponse createLedgerEntryFallback(LedgerTransferRequest request,
                                                             Throwable throwable) {
        log.error("ledgerApi Call Failed: {{ transferId: {}, error: {} }}",
                request.transferId(),
                throwable.getMessage());

        if (throwable instanceof TransferApplicationException e) {
            throw  e;
        }

        throw new ServiceUnavailableException("Ledger Service Unavailable", throwable);
    }
}
