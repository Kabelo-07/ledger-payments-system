package co.za.payments.transfers.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;
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
@EnableRetry
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
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        var registry = CircuitBreakerRegistry.ofDefaults();
        var cb = registry.circuitBreaker("LedgerApiCircuitBreaker");

        cb.getEventPublisher()
                .onStateTransition(event -> {
                    var transition = event.getStateTransition();
                    var failureRate = cb.getMetrics().getFailureRate();
                    var bufferedCalls = cb.getMetrics().getNumberOfBufferedCalls();

                    log.info("circuitBreakerStateChange: {{ from: {}, to: {}, failureRate: {}, buffered: {} }}",
                            transition.getFromState(),
                            transition.getToState(),
                            failureRate,
                            bufferedCalls);
                }).onCallNotPermitted(event -> {
                    var failureRate = cb.getMetrics().getFailureRate();
                    var bufferedCalls = cb.getMetrics().getNumberOfBufferedCalls();

                    log.warn("circuitBreakerCallBlocked: {{ failureRate: {}, buffered: {}  }}",
                            failureRate, bufferedCalls);
                });

        return registry;
    }
}
