package co.za.payments.ledger.api.controller;

import co.za.payments.ledger.domain.Account;
import co.za.payments.ledger.dto.CreateAccountRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static co.za.payments.ledger.config.AppConstants.ACCOUNT_NOT_FOUND_CODE;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerIT extends AbstractMvcIT {

    @Test
    void shouldReturn201Response_whenValidAccountRequestIsPosted() throws Exception {
        var request = new CreateAccountRequest(BigDecimal.valueOf(541));

        var resultActions = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andExpect(jsonPath("$.account_number", startsWith("ACC")))
                .andExpect(jsonPath("$.balance", equalTo(541)));
    }

    @Test
    void shouldReturn400Response_whenInitialBalanceIsZeroOrLess() throws Exception {
        var request = new CreateAccountRequest(BigDecimal.valueOf(0));

        var resultActions = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.errors", aMapWithSize(1)));
    }

    @Test
    void shouldReturn200Response_whenGivenAccountIdIsValid() throws Exception {
        var account = accountRepository.save(Account.instanceOf(BigDecimal.valueOf(981)));

        var resultActions = mockMvc.perform(get("/accounts/{id}", account.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andExpect(jsonPath("$.account_number", startsWith("ACC")));
    }

    @Test
    void shouldReturn404Response_whenGivenAccountIdIsInvalid() throws Exception {
        var resultActions = mockMvc.perform(get("/accounts/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(ACCOUNT_NOT_FOUND_CODE)))
                .andExpect(jsonPath("$.message", startsWith("Account with ID")));
    }
}