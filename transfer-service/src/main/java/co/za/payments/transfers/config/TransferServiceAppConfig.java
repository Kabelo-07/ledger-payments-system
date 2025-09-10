package co.za.payments.transfers.config;

import co.za.payments.transfers.client.LedgerFeignErrorDecoder;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Optional;
import java.util.concurrent.Executor;

import static co.za.payments.transfers.config.AppConstants.REQUEST_ID_HEADER_NAME;

@Configuration
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor
@Slf4j
public class TransferServiceAppConfig {

    private final TransferConfigProperties properties;

    @Profile("!test")
    @Bean(name = "transferExecutor")
    @Primary
    public Executor transferExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.threadPoolSize());
        executor.setMaxPoolSize(properties.threadPoolSize());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix("transfer-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new LedgerFeignErrorDecoder();
    }

    @Bean
    public RequestInterceptor correlationInterceptor() {
        return template -> Optional.ofNullable(MDC.get(REQUEST_ID_HEADER_NAME))
                .ifPresent(s -> template.header(REQUEST_ID_HEADER_NAME, s));
    }

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerLogger() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                CircuitBreaker cb = event.getAddedEntry();
                log.info("CircuitBreaker '{}' created", cb.getName());

                cb.getEventPublisher()
                        .onStateTransition(e -> log.info("CircuitBreaker: [{}], transition {} -> {}",
                                cb.getName(),
                                e.getStateTransition().getFromState(),
                                e.getStateTransition().getToState()))
                        .onCallNotPermitted(e -> log.warn("CircuitBreaker: [{}], call blocked", cb.getName()))
                        .onError(e -> log.error("CircuitBreaker: [{}], error {}", cb.getName(), e.getThrowable().toString()))
                        .onSuccess(e -> log.info("CircuitBreaker: [{}] success", cb.getName()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {}
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> event) {}
        };
    }
}
