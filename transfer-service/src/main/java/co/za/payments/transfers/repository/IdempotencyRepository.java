package co.za.payments.transfers.repository;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyRepository {

    <T> Optional<T> get(String key, Class<T> type);

    void put(String key, Object object, Duration ttl);
}
