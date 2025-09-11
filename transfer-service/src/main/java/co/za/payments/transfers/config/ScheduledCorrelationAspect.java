package co.za.payments.transfers.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Aspect
public class ScheduledCorrelationAspect {

    private static final String REQUEST_ID_HEADER_NAME = "X-Request-ID";

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object wrapScheduled(ProceedingJoinPoint joinPoint) throws Throwable {
        var correlationId = UUID.randomUUID().toString();

        MDC.put(REQUEST_ID_HEADER_NAME, correlationId);

        try {
            return joinPoint.proceed();
        } finally {
            MDC.remove(correlationId);
        }
    }

}
