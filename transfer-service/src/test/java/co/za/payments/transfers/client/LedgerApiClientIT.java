package co.za.payments.transfers.client;

import co.za.payments.transfers.client.contract.LedgerEntryDto;
import co.za.payments.transfers.client.contract.LedgerTransferResponse;
import co.za.payments.transfers.dto.LedgerTransferRequest;
import co.za.payments.transfers.exception.ErrorResponse;
import co.za.payments.transfers.exception.LedgerServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LedgerApiClientIT {

    @Autowired
    private LedgerApiClient ledgerApiClient;

    @Autowired
    private ObjectMapper objectMapper;

    static WireMockServer wireMockServer = new WireMockServer();

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void cleanUp() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("ledger.service.base-url", () -> wireMockServer.baseUrl());
    }

    @Test
    void shouldCallLedgerApiSuccessfully() throws JsonProcessingException {
        var transferId = UUID.randomUUID();
        var fromAccountId = UUID.randomUUID();
        var toAccountId = UUID.randomUUID();

        var req = new LedgerTransferRequest(transferId,
                fromAccountId, toAccountId, BigDecimal.valueOf(150));

        stubFor(post(urlEqualTo("/ledger/transfer"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(req)))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(
                                LedgerTransferResponse.builder()
                                        .createdAt(Instant.now())
                                        .transferId(transferId)
                                        .creditEntry(new LedgerEntryDto(toAccountId, BigDecimal.valueOf(150), "CREDIT"))
                                        .debitEntry(new LedgerEntryDto(fromAccountId, BigDecimal.valueOf(150), "DEBIT"))
                                        .build())))
        );

        // when
        var ledgerResponse = ledgerApiClient.createLedgerEntry(req);

        assertThat(ledgerResponse.transferId()).isEqualTo(transferId);
        assertThat(ledgerResponse.debitEntry().accountId()).isEqualTo(fromAccountId);
        assertThat(ledgerResponse.debitEntry().amount()).isEqualTo(req.amount());
        assertThat(ledgerResponse.creditEntry().accountId()).isEqualTo(toAccountId);
        assertThat(ledgerResponse.creditEntry().amount()).isEqualTo(req.amount());
    }

    @Test
    void shouldFallbackWhenLedgerApiInvocationFails() throws JsonProcessingException {
        var transferId = UUID.randomUUID();
        var fromAccountId = UUID.randomUUID();
        var toAccountId = UUID.randomUUID();

        var req = new LedgerTransferRequest(transferId,
                fromAccountId, toAccountId, BigDecimal.valueOf(150));

        stubFor(post(urlEqualTo("/ledger/transfer"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(req)))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(
                                new ErrorResponse(
                                        500,
                                        "ERROR",
                                        "error occurred"))))
        );

        // when
        assertThatExceptionOfType(LedgerServiceException.class)
                .isThrownBy(() -> ledgerApiClient.createLedgerEntry(req));
    }

}