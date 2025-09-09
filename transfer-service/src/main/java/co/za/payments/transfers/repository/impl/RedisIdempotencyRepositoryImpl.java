package co.za.payments.transfers.repository.impl;

import co.za.payments.transfers.exception.TransferApplicationException;
import co.za.payments.transfers.repository.IdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyRepositoryImpl implements IdempotencyRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        if (StringUtils.isBlank(key)) {
            return Optional.empty();
        }

        var jsonStr = redisTemplate.opsForValue().get(key);

        if (StringUtils.isBlank(jsonStr)) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(jsonStr, type));
        } catch (Exception e) {
            log.warn("Unable to map json response, error: {}", e.getMessage());

            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object object, Duration ttl) {
        if (StringUtils.isBlank(key)) {
            return;
        }

        try {
            var jsonStr = objectMapper.writeValueAsString(object);
            redisTemplate.opsForValue().set(key, jsonStr, ttl);
        } catch (Exception e) {
            log.error("Unable to cache json object, error: {}", e.getMessage());

            throw new TransferApplicationException("IDEMPOTENCY_ERROR", "Unable to process data");
        }
    }

}
