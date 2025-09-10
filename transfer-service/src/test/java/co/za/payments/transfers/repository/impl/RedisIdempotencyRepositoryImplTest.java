package co.za.payments.transfers.repository.impl;

import co.za.payments.transfers.domain.TransferStatus;
import co.za.payments.transfers.dto.TransferResponse;
import co.za.payments.transfers.exception.TransferApplicationException;
import co.za.payments.transfers.repository.IdempotencyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import joptsimple.internal.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyRepositoryImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;

    private IdempotencyRepository idempotencyRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        idempotencyRepository = new RedisIdempotencyRepositoryImpl(redisTemplate, objectMapper);
    }
    @Test
    void shouldReturnEmptyResponse_whenRequestedKeyIsNull() {
        // given
        String key = null;

        // when
        var response = idempotencyRepository.get(key, TransferResponse.class);

        // then
        assertThat(response).isNotPresent();
    }

    @Test
    void shouldReturnEmptyResponseWhenCacheProcessingFailed() {
        // given
        String key = UUID.randomUUID().toString();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().get(key)).thenReturn("payload");

        // when
        var response = idempotencyRepository.get(key, TransferResponse.class);

        // then
        assertThat(response).isNotPresent();
    }

    @Test
    void shouldReturnEmptyResponse_whenRequestedKeyIsEmpty() {
        var key = "      ";

        // when
        var response = idempotencyRepository.get(Strings.EMPTY, TransferResponse.class);

        // then
        assertThat(response).isNotPresent();
    }

    @Test
    void shouldReturnValidResponse_whenRequestedKeyIsPresent() throws JsonProcessingException {
        // given
        var key = UUID.randomUUID().toString();

        // given
        var response = new TransferResponse(UUID.randomUUID(),
                TransferStatus.COMPLETED.name(),
                Instant.now(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.TEN);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().get(key)).thenReturn(objectMapper.writeValueAsString(response));

        // when
        var cachedResponse = idempotencyRepository.get(key, TransferResponse.class);

        // then
        assertThat(cachedResponse).isPresent();
        assertThat(cachedResponse.get().amount()).isEqualTo(BigDecimal.TEN);

    }

    @Test
    void shouldReturnEmptyResponse_whenCachedValueIsInvalidForKey() throws JsonProcessingException {
        // given
        var key = UUID.randomUUID().toString();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().get(key)).thenReturn("");

        // when
        var cachedResponse = idempotencyRepository.get(key, TransferResponse.class);

        // then
        assertThat(cachedResponse).isEmpty();
    }

    @Test
    void shouldCacheResponseWhenKeyAndResponseValid() throws JsonProcessingException {
        // given
        var key = UUID.randomUUID().toString();
        var response = new TransferResponse(UUID.randomUUID(),
                TransferStatus.COMPLETED.name(),
                Instant.now(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.TEN);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when // then
        idempotencyRepository.put(key, objectMapper.writeValueAsString(response), Duration.ZERO);
    }

    @Test
    void shouldThrowExceptionWhenCachingResponseFails() throws JsonProcessingException {
        // given
        var key = UUID.randomUUID().toString();

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException());

        // when
        assertThatExceptionOfType(TransferApplicationException.class)
                .isThrownBy(() -> idempotencyRepository.put(key, null, Duration.ZERO));

    }

    @Test
    void shouldDoNothingWhenKeyIsBlank() throws JsonProcessingException {
        // given
        var key = " ";

        // when
        idempotencyRepository.put(key, null, Duration.ZERO);

        // then
        verify(redisTemplate, never()).opsForValue();
    }

}