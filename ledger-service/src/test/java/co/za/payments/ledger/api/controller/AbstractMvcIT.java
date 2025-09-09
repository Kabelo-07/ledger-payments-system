package co.za.payments.ledger.api.controller;

import co.za.payments.ledger.repository.AccountRepository;
import co.za.payments.ledger.repository.LedgerEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractMvcIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LedgerEntryRepository ledgerRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    ObjectMapper mapper;

    @AfterEach
    void cleanUp() {
        ledgerRepository.deleteAll();
        accountRepository.deleteAll();
    }

}
