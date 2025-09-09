package co.za.payments.transfers.api;

import co.za.payments.transfers.domain.Transfer;
import co.za.payments.transfers.dto.AccountTransferRequest;
import co.za.payments.transfers.dto.TransferResponse;
import co.za.payments.transfers.repository.IdempotencyRepository;
import co.za.payments.transfers.repository.OutboxRepository;
import co.za.payments.transfers.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static co.za.payments.transfers.config.AppConstants.MISSING_HEADER;
import static co.za.payments.transfers.config.AppConstants.TRANSFER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class TransferControllerIT {

    @Container
    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private ObjectMapper mapper;

    private final UUID fromAccountId = UUID.randomUUID();
    private final UUID toAccountId = UUID.randomUUID();
    private final BigDecimal amount = BigDecimal.valueOf(588);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
        transferRepository.deleteAll();
    }

    @Test
    void shouldReturnHttp400ResponseWhenIdempotencyHeaderIsMissing() throws Exception {
        // given
        var amount = BigDecimal.valueOf(220);

        var transferRequest = new AccountTransferRequest(fromAccountId, toAccountId, amount);

        // when
        var resultActions = mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(transferRequest)));

        // then
        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(MISSING_HEADER)))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturnHttp201ResponseWhenValidTransferIsInitiated() throws Exception {
        // given
        var transferRequest = new AccountTransferRequest(fromAccountId, toAccountId, amount);

        // when
        var resultActions = mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                .content(mapper.writeValueAsString(transferRequest)));

        // then
        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.transfer_id").isNotEmpty())
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andExpect(jsonPath("$.from_account_id", equalTo(String.valueOf(fromAccountId))))
                .andExpect(jsonPath("$.status", equalTo("PROCESSING")))
                .andExpect(jsonPath("$.to_account_id", equalTo(String.valueOf(toAccountId))))
                .andExpect(jsonPath("$.amount", equalTo(588)));
    }

    @Test
    void shouldReturn201_andSameResponse_whenDuplicateIdempotencyKey() throws Exception {
        // given
        var idempotencyKey = UUID.randomUUID().toString();

        // when first call is made
        var transferRequest = new AccountTransferRequest(fromAccountId, toAccountId, BigDecimal.valueOf(17_000));
        var firstMvcResults = mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(mapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // when second call is made
        var secondTransferRequest = new AccountTransferRequest(fromAccountId, toAccountId, BigDecimal.valueOf(9_900));
        var secondMvcResults = mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(mapper.writeValueAsString(secondTransferRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var firstResponse = mapper.readValue(firstMvcResults.getResponse().getContentAsString(), TransferResponse.class);
        var secondResponse = mapper.readValue(secondMvcResults.getResponse().getContentAsString(), TransferResponse.class);

        // then first and second response are similar
        assertThat(firstResponse).isEqualTo(secondResponse);

        // and check only 1 transfer exists
        assertThat(transferRepository.findAll()).hasSize(1);
        // and only 1 outbox event exists
        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldReturn201_andDifferentResponse_whenDifferentIdempotencyKeys() throws Exception {
        // given
        var transferRequest = new AccountTransferRequest(fromAccountId, toAccountId, BigDecimal.valueOf(17_000));

        // when first call is made
        var firstMvcResults = mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(mapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // when second call is made
        var secondTransferRequest = new AccountTransferRequest(fromAccountId, toAccountId, BigDecimal.valueOf(9_900));
        var secondMvcResults = mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .content(mapper.writeValueAsString(secondTransferRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var firstResponse = mapper.readValue(firstMvcResults.getResponse().getContentAsString(), TransferResponse.class);
        var secondResponse = mapper.readValue(secondMvcResults.getResponse().getContentAsString(), TransferResponse.class);

        // then first and second response are NOT similar
        assertThat(firstResponse).isNotEqualTo(secondResponse);

        // and two transfers exist
        assertThat(transferRepository.findAll()).hasSize(2);

        // and 2 outbox events exist
        assertThat(outboxRepository.findAll()).hasSize(2);
    }

    @Test
    void shouldReturnHttp200ResponseWhenValidTransferIdProvided() throws Exception {
        // given
        var amount = BigDecimal.valueOf(200);

        // and
        var transfer = transferRepository.save(Transfer.instanceOf(fromAccountId, amount, toAccountId));

        // when
        var resultActions = mockMvc.perform(get("/transfers/{id}", transfer.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.transfer_id", equalTo(String.valueOf(transfer.getId()))))
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andExpect(jsonPath("$.from_account_id", equalTo(String.valueOf(fromAccountId))))
                .andExpect(jsonPath("$.status", equalTo("PROCESSING")))
                .andExpect(jsonPath("$.to_account_id", equalTo(String.valueOf(toAccountId))))
                .andExpect(jsonPath("$.amount", equalTo(200.00)));
    }

    @Test
    void shouldReturnHttp404ResponseWhenInvalidTransferIdProvided() throws Exception {
        // given
        var transferId = UUID.randomUUID();

        // when
        var resultActions = mockMvc.perform(get("/transfers/{id}", transferId)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(TRANSFER_NOT_FOUND)))
                .andExpect(jsonPath("$.message", startsWith("Transfer with ID")));
    }
}