package co.za.payments.ledger.api.controller;


import co.za.payments.ledger.service.AccountService;
import co.za.payments.ledger.dto.AccountResponse;
import co.za.payments.ledger.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody @Valid CreateAccountRequest request) {
        var createdAccountDto = accountService.create(request);
        return ResponseEntity.created(URI.create("/accounts/%s".formatted(createdAccountDto.id())))
                .body(createdAccountDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }


}
